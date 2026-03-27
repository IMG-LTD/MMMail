package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.LumoConversationMapper;
import com.mmmail.server.mapper.LumoMessageMapper;
import com.mmmail.server.mapper.LumoProjectKnowledgeMapper;
import com.mmmail.server.mapper.LumoProjectMapper;
import com.mmmail.server.model.entity.LumoConversation;
import com.mmmail.server.model.entity.LumoMessage;
import com.mmmail.server.model.entity.LumoProject;
import com.mmmail.server.model.entity.LumoProjectKnowledge;
import com.mmmail.server.model.vo.LumoConversationVo;
import com.mmmail.server.model.vo.LumoMessageVo;
import com.mmmail.server.model.vo.LumoProjectKnowledgeVo;
import com.mmmail.server.model.vo.LumoProjectVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class LumoService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";
    private static final int DEFAULT_CONVERSATION_LIMIT = 50;
    private static final int DEFAULT_MESSAGE_LIMIT = 100;

    private final LumoConversationMapper lumoConversationMapper;
    private final LumoMessageMapper lumoMessageMapper;
    private final LumoProjectMapper lumoProjectMapper;
    private final LumoProjectKnowledgeMapper lumoProjectKnowledgeMapper;
    private final AuditService auditService;
    private final LumoWorkspaceSupportService workspaceSupportService;
    private final LumoCapabilityService lumoCapabilityService;

    public LumoService(
            LumoConversationMapper lumoConversationMapper,
            LumoMessageMapper lumoMessageMapper,
            LumoProjectMapper lumoProjectMapper,
            LumoProjectKnowledgeMapper lumoProjectKnowledgeMapper,
            AuditService auditService,
            LumoWorkspaceSupportService workspaceSupportService,
            LumoCapabilityService lumoCapabilityService
    ) {
        this.lumoConversationMapper = lumoConversationMapper;
        this.lumoMessageMapper = lumoMessageMapper;
        this.lumoProjectMapper = lumoProjectMapper;
        this.lumoProjectKnowledgeMapper = lumoProjectKnowledgeMapper;
        this.auditService = auditService;
        this.workspaceSupportService = workspaceSupportService;
        this.lumoCapabilityService = lumoCapabilityService;
    }

    public List<LumoProjectVo> listProjects(Long userId, Integer limit, String ipAddress) {
        int safeLimit = workspaceSupportService.safeLimit(limit, DEFAULT_CONVERSATION_LIMIT);
        List<LumoProjectVo> projects = lumoProjectMapper.selectList(new LambdaQueryWrapper<LumoProject>()
                        .eq(LumoProject::getOwnerId, userId)
                        .orderByDesc(LumoProject::getUpdatedAt)
                        .last("limit " + safeLimit))
                .stream()
                .map(project -> workspaceSupportService.toProjectVo(project, countConversationsForProject(userId, project.getId())))
                .toList();

        auditService.record(userId, "LUMO_PROJECT_LIST", "count=" + projects.size(), ipAddress);
        return projects;
    }

    @Transactional
    public LumoProjectVo createProject(Long userId, String name, String description, String ipAddress) {
        String safeName = workspaceSupportService.requireProjectName(name);
        String safeDescription = workspaceSupportService.normalizeProjectDescription(description);

        LumoProject existing = lumoProjectMapper.selectOne(new LambdaQueryWrapper<LumoProject>()
                .eq(LumoProject::getOwnerId, userId)
                .eq(LumoProject::getName, safeName));
        if (existing != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo project name already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        LumoProject project = new LumoProject();
        project.setOwnerId(userId);
        project.setName(safeName);
        project.setDescription(safeDescription);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        project.setDeleted(0);
        lumoProjectMapper.insert(project);

        auditService.record(userId, "LUMO_PROJECT_CREATE", "projectId=" + project.getId(), ipAddress);
        return workspaceSupportService.toProjectVo(project, 0L);
    }

    public List<LumoProjectKnowledgeVo> listProjectKnowledge(
            Long userId,
            Long projectId,
            Integer limit,
            String ipAddress
    ) {
        LumoProject project = loadProject(userId, projectId);
        int safeLimit = workspaceSupportService.safeLimit(limit, DEFAULT_MESSAGE_LIMIT);
        List<LumoProjectKnowledgeVo> items = lumoProjectKnowledgeMapper.selectList(new LambdaQueryWrapper<LumoProjectKnowledge>()
                        .eq(LumoProjectKnowledge::getOwnerId, userId)
                        .eq(LumoProjectKnowledge::getProjectId, project.getId())
                        .orderByDesc(LumoProjectKnowledge::getUpdatedAt)
                        .last("limit " + safeLimit))
                .stream()
                .map(workspaceSupportService::toProjectKnowledgeVo)
                .toList();

        auditService.record(
                userId,
                "LUMO_KNOWLEDGE_LIST",
                "projectId=" + project.getId() + ",count=" + items.size(),
                ipAddress
        );
        return items;
    }

    @Transactional
    public LumoProjectKnowledgeVo createProjectKnowledge(
            Long userId,
            Long projectId,
            String title,
            String content,
            String ipAddress
    ) {
        LumoProject project = loadProject(userId, projectId);
        String safeTitle = workspaceSupportService.requireKnowledgeTitle(title);
        String safeContent = workspaceSupportService.requireKnowledgeContent(content);
        LocalDateTime now = LocalDateTime.now();

        LumoProjectKnowledge knowledge = new LumoProjectKnowledge();
        knowledge.setOwnerId(userId);
        knowledge.setProjectId(project.getId());
        knowledge.setTitle(safeTitle);
        knowledge.setContent(safeContent);
        knowledge.setCreatedAt(now);
        knowledge.setUpdatedAt(now);
        knowledge.setDeleted(0);
        lumoProjectKnowledgeMapper.insert(knowledge);

        auditService.record(
                userId,
                "LUMO_KNOWLEDGE_CREATE",
                "projectId=" + project.getId() + ",knowledgeId=" + knowledge.getId(),
                ipAddress
        );
        return workspaceSupportService.toProjectKnowledgeVo(knowledge);
    }

    @Transactional
    public void deleteProjectKnowledge(
            Long userId,
            Long projectId,
            Long knowledgeId,
            String ipAddress
    ) {
        LumoProjectKnowledge knowledge = loadProjectKnowledge(userId, projectId, knowledgeId);
        lumoProjectKnowledgeMapper.deleteById(knowledge.getId());
        auditService.record(
                userId,
                "LUMO_KNOWLEDGE_DELETE",
                "projectId=" + knowledge.getProjectId() + ",knowledgeId=" + knowledge.getId(),
                ipAddress
        );
    }

    public List<LumoConversationVo> listConversations(
            Long userId,
            Long projectId,
            Boolean includeArchived,
            Integer limit,
            String ipAddress
    ) {
        int safeLimit = workspaceSupportService.safeLimit(limit, DEFAULT_CONVERSATION_LIMIT);
        LambdaQueryWrapper<LumoConversation> queryWrapper = new LambdaQueryWrapper<LumoConversation>()
                .eq(LumoConversation::getOwnerId, userId)
                .orderByDesc(LumoConversation::getUpdatedAt)
                .last("limit " + safeLimit);

        if (projectId != null) {
            queryWrapper.eq(LumoConversation::getProjectId, projectId);
        }
        if (!Boolean.TRUE.equals(includeArchived)) {
            queryWrapper.eq(LumoConversation::getArchived, 0);
        }

        List<LumoConversationVo> conversations = lumoConversationMapper.selectList(queryWrapper).stream()
                .map(workspaceSupportService::toConversationVo)
                .toList();

        auditService.record(
                userId,
                "LUMO_CONVERSATION_LIST",
                "count=" + conversations.size() + ",includeArchived=" + Boolean.TRUE.equals(includeArchived),
                ipAddress
        );
        return conversations;
    }

    @Transactional
    public LumoConversationVo createConversation(
            Long userId,
            String title,
            String modelCode,
            Long projectId,
            String ipAddress
    ) {
        String safeTitle = workspaceSupportService.requireTitle(title);
        String safeModelCode = workspaceSupportService.normalizeModelCode(modelCode);
        if (projectId != null) {
            loadProject(userId, projectId);
        }

        LocalDateTime now = LocalDateTime.now();
        LumoConversation conversation = new LumoConversation();
        conversation.setOwnerId(userId);
        conversation.setProjectId(projectId);
        conversation.setTitle(safeTitle);
        conversation.setPinned(0);
        conversation.setModelCode(safeModelCode);
        conversation.setArchived(0);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setDeleted(0);
        lumoConversationMapper.insert(conversation);

        auditService.record(userId, "LUMO_CONVERSATION_CREATE", "conversationId=" + conversation.getId(), ipAddress);
        return workspaceSupportService.toConversationVo(conversation);
    }

    @Transactional
    public LumoConversationVo updateConversationModel(Long userId, Long conversationId, String modelCode, String ipAddress) {
        LumoConversation conversation = loadConversation(userId, conversationId);
        String safeModelCode = workspaceSupportService.normalizeModelCode(modelCode);
        conversation.setModelCode(safeModelCode);
        conversation.setUpdatedAt(LocalDateTime.now());
        lumoConversationMapper.updateById(conversation);

        auditService.record(
                userId,
                "LUMO_CONVERSATION_MODEL_UPDATE",
                "conversationId=" + conversation.getId() + ",model=" + safeModelCode,
                ipAddress
        );
        return workspaceSupportService.toConversationVo(conversation);
    }

    @Transactional
    public LumoConversationVo archiveConversation(Long userId, Long conversationId, Boolean archived, String ipAddress) {
        LumoConversation conversation = loadConversation(userId, conversationId);
        int archivedFlag = Boolean.TRUE.equals(archived) ? 1 : 0;
        conversation.setArchived(archivedFlag);
        conversation.setUpdatedAt(LocalDateTime.now());
        lumoConversationMapper.updateById(conversation);

        auditService.record(
                userId,
                "LUMO_CONVERSATION_ARCHIVE",
                "conversationId=" + conversation.getId() + ",archived=" + archivedFlag,
                ipAddress
        );
        return workspaceSupportService.toConversationVo(conversation);
    }

    public List<LumoMessageVo> listMessages(Long userId, Long conversationId, Integer limit, String ipAddress) {
        LumoConversation conversation = loadConversation(userId, conversationId);
        int safeLimit = workspaceSupportService.safeLimit(limit, DEFAULT_MESSAGE_LIMIT);

        List<LumoMessageVo> messages = lumoMessageMapper.selectList(new LambdaQueryWrapper<LumoMessage>()
                        .eq(LumoMessage::getOwnerId, userId)
                        .eq(LumoMessage::getConversationId, conversation.getId())
                        .orderByDesc(LumoMessage::getCreatedAt)
                        .last("limit " + safeLimit))
                .stream()
                .sorted(Comparator.comparing(LumoMessage::getCreatedAt))
                .map(workspaceSupportService::toMessageVo)
                .toList();

        auditService.record(
                userId,
                "LUMO_MESSAGE_LIST",
                "conversationId=" + conversation.getId() + ",count=" + messages.size(),
                ipAddress
        );
        return messages;
    }

    @Transactional
    public List<LumoMessageVo> sendMessage(
            Long userId,
            Long conversationId,
            String content,
            List<Long> knowledgeIds,
            Boolean webSearchEnabled,
            Boolean citationsEnabled,
            String translateToLocale,
            String ipAddress
    ) {
        LumoConversation conversation = loadConversation(userId, conversationId);
        if (workspaceSupportService.safeFlag(conversation.getArchived()) == 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Archived conversation cannot receive new messages");
        }

        String safeContent = workspaceSupportService.requireContent(content);
        List<LumoProjectKnowledge> referencedKnowledge = resolveReferencedKnowledge(
                userId,
                conversation.getProjectId(),
                knowledgeIds
        );
        LocalDateTime now = LocalDateTime.now();

        LumoMessage userMessage = new LumoMessage();
        userMessage.setOwnerId(userId);
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole(ROLE_USER);
        userMessage.setContent(safeContent);
        userMessage.setCapabilityPayloadJson(null);
        userMessage.setTokenCount(workspaceSupportService.estimateTokenCount(safeContent));
        userMessage.setCreatedAt(now);
        userMessage.setUpdatedAt(now);
        userMessage.setDeleted(0);
        lumoMessageMapper.insert(userMessage);

        LumoCapabilityService.AssistantReply assistantReply = lumoCapabilityService.composeAssistantReply(
                safeContent,
                workspaceSupportService.normalizeModelCode(conversation.getModelCode()),
                referencedKnowledge,
                Boolean.TRUE.equals(webSearchEnabled),
                Boolean.TRUE.equals(citationsEnabled),
                translateToLocale
        );

        LumoMessage assistantMessage = new LumoMessage();
        assistantMessage.setOwnerId(userId);
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setRole(ROLE_ASSISTANT);
        assistantMessage.setContent(assistantReply.content());
        assistantMessage.setCapabilityPayloadJson(assistantReply.capabilityPayloadJson());
        assistantMessage.setTokenCount(workspaceSupportService.estimateTokenCount(assistantReply.content()));
        assistantMessage.setCreatedAt(now);
        assistantMessage.setUpdatedAt(now);
        assistantMessage.setDeleted(0);
        lumoMessageMapper.insert(assistantMessage);

        conversation.setUpdatedAt(now);
        lumoConversationMapper.updateById(conversation);

        auditService.record(
                userId,
                "LUMO_MESSAGE_SEND",
                "conversationId=" + conversation.getId()
                        + ",userMessageId=" + userMessage.getId()
                        + ",modelCode=" + conversation.getModelCode()
                        + ",knowledgeCount=" + referencedKnowledge.size()
                        + ",webSearch=" + Boolean.TRUE.equals(webSearchEnabled)
                        + ",citations=" + Boolean.TRUE.equals(citationsEnabled)
                        + ",translate=" + (translateToLocale == null ? "" : translateToLocale),
                ipAddress
        );
        return List.of(
                workspaceSupportService.toMessageVo(userMessage),
                workspaceSupportService.toMessageVo(assistantMessage)
        );
    }

    private LumoConversation loadConversation(Long userId, Long conversationId) {
        if (conversationId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo conversationId is required");
        }
        LumoConversation conversation = lumoConversationMapper.selectOne(new LambdaQueryWrapper<LumoConversation>()
                .eq(LumoConversation::getId, conversationId)
                .eq(LumoConversation::getOwnerId, userId));
        if (conversation == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo conversation is not found");
        }
        return conversation;
    }

    private LumoProject loadProject(Long userId, Long projectId) {
        if (projectId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo projectId is required");
        }
        LumoProject project = lumoProjectMapper.selectOne(new LambdaQueryWrapper<LumoProject>()
                .eq(LumoProject::getId, projectId)
                .eq(LumoProject::getOwnerId, userId));
        if (project == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo project is not found");
        }
        return project;
    }

    private LumoProjectKnowledge loadProjectKnowledge(Long userId, Long projectId, Long knowledgeId) {
        if (knowledgeId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo knowledgeId is required");
        }
        LumoProjectKnowledge knowledge = lumoProjectKnowledgeMapper.selectOne(new LambdaQueryWrapper<LumoProjectKnowledge>()
                .eq(LumoProjectKnowledge::getId, knowledgeId)
                .eq(LumoProjectKnowledge::getOwnerId, userId)
                .eq(LumoProjectKnowledge::getProjectId, projectId));
        if (knowledge == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo knowledge source is not found");
        }
        return knowledge;
    }

    private List<LumoProjectKnowledge> resolveReferencedKnowledge(
            Long userId,
            Long projectId,
            List<Long> knowledgeIds
    ) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return List.of();
        }
        if (projectId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Lumo conversation has no project to bind knowledge");
        }
        List<Long> deduplicatedIds = knowledgeIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .limit(20)
                .toList();
        if (deduplicatedIds.isEmpty()) {
            return List.of();
        }
        List<LumoProjectKnowledge> rows = lumoProjectKnowledgeMapper.selectList(new LambdaQueryWrapper<LumoProjectKnowledge>()
                .eq(LumoProjectKnowledge::getOwnerId, userId)
                .eq(LumoProjectKnowledge::getProjectId, projectId)
                .in(LumoProjectKnowledge::getId, deduplicatedIds));
        if (rows.size() != deduplicatedIds.size()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Some Lumo knowledge sources are invalid");
        }
        return rows;
    }

    private long countConversationsForProject(Long userId, Long projectId) {
        Long count = lumoConversationMapper.selectCount(new LambdaQueryWrapper<LumoConversation>()
                .eq(LumoConversation::getOwnerId, userId)
                .eq(LumoConversation::getProjectId, projectId));
        return count == null ? 0L : count;
    }
}
