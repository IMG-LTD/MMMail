package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateLumoConversationRequest;
import com.mmmail.server.model.dto.CreateLumoProjectKnowledgeRequest;
import com.mmmail.server.model.dto.CreateLumoProjectRequest;
import com.mmmail.server.model.dto.SendLumoMessageRequest;
import com.mmmail.server.model.dto.UpdateLumoConversationArchiveRequest;
import com.mmmail.server.model.dto.UpdateLumoConversationModelRequest;
import com.mmmail.server.model.vo.LumoConversationVo;
import com.mmmail.server.model.vo.LumoMessageVo;
import com.mmmail.server.model.vo.LumoProjectKnowledgeVo;
import com.mmmail.server.model.vo.LumoProjectVo;
import com.mmmail.server.service.LumoService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lumo")
public class LumoController {

    private final LumoService lumoService;

    public LumoController(LumoService lumoService) {
        this.lumoService = lumoService;
    }

    @GetMapping("/projects")
    public Result<List<LumoProjectVo>> listProjects(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.listProjects(
                SecurityUtils.currentUserId(),
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/projects")
    public Result<LumoProjectVo> createProject(
            @Valid @RequestBody CreateLumoProjectRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.createProject(
                SecurityUtils.currentUserId(),
                request.name(),
                request.description(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/projects/{projectId}/knowledge")
    public Result<List<LumoProjectKnowledgeVo>> listProjectKnowledge(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.listProjectKnowledge(
                SecurityUtils.currentUserId(),
                projectId,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/projects/{projectId}/knowledge")
    public Result<LumoProjectKnowledgeVo> createProjectKnowledge(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateLumoProjectKnowledgeRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.createProjectKnowledge(
                SecurityUtils.currentUserId(),
                projectId,
                request.title(),
                request.content(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/projects/{projectId}/knowledge/{knowledgeId}")
    public Result<Boolean> deleteProjectKnowledge(
            @PathVariable Long projectId,
            @PathVariable Long knowledgeId,
            HttpServletRequest httpRequest
    ) {
        lumoService.deleteProjectKnowledge(
                SecurityUtils.currentUserId(),
                projectId,
                knowledgeId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(Boolean.TRUE);
    }

    @GetMapping("/conversations")
    public Result<List<LumoConversationVo>> listConversations(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.listConversations(
                SecurityUtils.currentUserId(),
                projectId,
                includeArchived,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/conversations")
    public Result<LumoConversationVo> createConversation(
            @Valid @RequestBody CreateLumoConversationRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.createConversation(
                SecurityUtils.currentUserId(),
                request.title(),
                request.modelCode(),
                request.projectId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/conversations/{conversationId}/model")
    public Result<LumoConversationVo> updateConversationModel(
            @PathVariable Long conversationId,
            @Valid @RequestBody UpdateLumoConversationModelRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.updateConversationModel(
                SecurityUtils.currentUserId(),
                conversationId,
                request.modelCode(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/conversations/{conversationId}/archive")
    public Result<LumoConversationVo> archiveConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody UpdateLumoConversationArchiveRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.archiveConversation(
                SecurityUtils.currentUserId(),
                conversationId,
                request.archived(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<LumoMessageVo>> listMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.listMessages(
                SecurityUtils.currentUserId(),
                conversationId,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public Result<List<LumoMessageVo>> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendLumoMessageRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(lumoService.sendMessage(
                SecurityUtils.currentUserId(),
                conversationId,
                request.content(),
                request.knowledgeIds(),
                request.webSearchEnabled(),
                request.citationsEnabled(),
                request.translateToLocale(),
                httpRequest.getRemoteAddr()
        ));
    }
}
