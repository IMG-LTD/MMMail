package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.LumoConversation;
import com.mmmail.server.model.entity.LumoMessage;
import com.mmmail.server.model.entity.LumoProject;
import com.mmmail.server.model.entity.LumoProjectKnowledge;
import com.mmmail.server.model.vo.LumoCitationVo;
import com.mmmail.server.model.vo.LumoConversationVo;
import com.mmmail.server.model.vo.LumoMessageVo;
import com.mmmail.server.model.vo.LumoProjectKnowledgeVo;
import com.mmmail.server.model.vo.LumoProjectVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LumoWorkspaceSupportService {

    private static final int MAX_LIMIT = 200;
    private static final String MODEL_BASE = "LUMO-BASE";
    private static final Set<String> SUPPORTED_MODELS = Set.of(MODEL_BASE, "LUMO-PLUS", "LUMO-BIZ");

    private final LumoCapabilityService lumoCapabilityService;

    public LumoWorkspaceSupportService(LumoCapabilityService lumoCapabilityService) {
        this.lumoCapabilityService = lumoCapabilityService;
    }

    public String defaultModelCode() {
        return MODEL_BASE;
    }

    public String requireTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo conversation title is required");
        }
        String safeTitle = title.trim();
        if (safeTitle.length() < 2 || safeTitle.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo conversation title length is invalid");
        }
        return safeTitle;
    }

    public String requireProjectName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo project name is required");
        }
        String safeName = name.trim();
        if (safeName.length() < 2 || safeName.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo project name length is invalid");
        }
        return safeName;
    }

    public String normalizeProjectDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return "";
        }
        String safeDescription = description.trim();
        if (safeDescription.length() > 256) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo project description is too long");
        }
        return safeDescription;
    }

    public String requireKnowledgeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo knowledge title is required");
        }
        String safeTitle = title.trim();
        if (safeTitle.length() < 2 || safeTitle.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo knowledge title length is invalid");
        }
        return safeTitle;
    }

    public String requireKnowledgeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo knowledge content is required");
        }
        String safeContent = content.trim();
        if (safeContent.length() > 2000) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo knowledge content is too long");
        }
        return safeContent;
    }

    public String normalizeModelCode(String modelCode) {
        if (!StringUtils.hasText(modelCode)) {
            return MODEL_BASE;
        }
        String safeModelCode = modelCode.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_MODELS.contains(safeModelCode)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo modelCode is unsupported");
        }
        return safeModelCode;
    }

    public String requireContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo message content is required");
        }
        String safeContent = content.trim();
        if (safeContent.length() > 4000) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo message content is too long");
        }
        return safeContent;
    }

    public int estimateTokenCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return Math.max(1, content.trim().split("\\s+").length);
    }

    public int safeFlag(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }

    public int safeLimit(Integer limit, int defaultLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1) {
            return 1;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
    }

    public LumoConversationVo toConversationVo(LumoConversation conversation) {
        return new LumoConversationVo(
                String.valueOf(conversation.getId()),
                conversation.getProjectId() == null ? null : String.valueOf(conversation.getProjectId()),
                conversation.getTitle(),
                conversation.getPinned() != null && conversation.getPinned() == 1,
                normalizeModelCode(conversation.getModelCode()),
                safeFlag(conversation.getArchived()) == 1,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public LumoProjectVo toProjectVo(LumoProject project, long conversationCount) {
        return new LumoProjectVo(
                String.valueOf(project.getId()),
                project.getName(),
                project.getDescription(),
                conversationCount,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public LumoProjectKnowledgeVo toProjectKnowledgeVo(LumoProjectKnowledge knowledge) {
        return new LumoProjectKnowledgeVo(
                String.valueOf(knowledge.getId()),
                String.valueOf(knowledge.getProjectId()),
                knowledge.getTitle(),
                knowledge.getContent(),
                knowledge.getCreatedAt(),
                knowledge.getUpdatedAt()
        );
    }

    public LumoMessageVo toMessageVo(LumoMessage message) {
        LumoCapabilityService.MessageCapabilityView payload = lumoCapabilityService.resolveView(message.getCapabilityPayloadJson());
        List<LumoCitationVo> citations = payload.citations().stream()
                .map(item -> new LumoCitationVo(item.title(), item.url(), item.note(), item.sourceType()))
                .toList();
        return new LumoMessageVo(
                String.valueOf(message.getId()),
                String.valueOf(message.getConversationId()),
                message.getRole(),
                message.getContent(),
                message.getTokenCount() == null ? 0 : message.getTokenCount(),
                message.getCreatedAt(),
                payload.capabilityMode(),
                payload.responseLocale(),
                payload.webSearchEnabled(),
                payload.citationsEnabled(),
                citations
        );
    }
}
