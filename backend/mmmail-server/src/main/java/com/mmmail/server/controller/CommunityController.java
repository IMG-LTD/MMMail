package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateCommunityCommentRequest;
import com.mmmail.server.model.dto.CreateCommunityPostRequest;
import com.mmmail.server.model.dto.CreateCommunityReportRequest;
import com.mmmail.server.model.dto.CreateCommunityTopicRequest;
import com.mmmail.server.model.dto.ModerateCommunityReportRequest;
import com.mmmail.server.model.dto.UpdateCommunityPostRequest;
import com.mmmail.server.model.dto.UpdateCommunityTopicRequest;
import com.mmmail.server.model.vo.CommunityBookmarkVo;
import com.mmmail.server.model.vo.CommunityCommentVo;
import com.mmmail.server.model.vo.CommunityPostPageVo;
import com.mmmail.server.model.vo.CommunityPostVo;
import com.mmmail.server.model.vo.CommunityReactionVo;
import com.mmmail.server.model.vo.CommunityReportPageVo;
import com.mmmail.server.model.vo.CommunityReportVo;
import com.mmmail.server.model.vo.CommunityTopicDeleteVo;
import com.mmmail.server.model.vo.CommunityTopicVo;
import com.mmmail.server.model.vo.CommunityViewVo;
import com.mmmail.server.service.CommunityEngagementService;
import com.mmmail.server.service.CommunityModerationService;
import com.mmmail.server.service.CommunityService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community")
public class CommunityController {

    private final CommunityService communityService;
    private final CommunityEngagementService engagementService;
    private final CommunityModerationService moderationService;

    public CommunityController(
            CommunityService communityService,
            CommunityEngagementService engagementService,
            CommunityModerationService moderationService
    ) {
        this.communityService = communityService;
        this.engagementService = engagementService;
        this.moderationService = moderationService;
    }

    @GetMapping("/topics")
    public Result<List<CommunityTopicVo>> topics() {
        return Result.success(communityService.listTopics());
    }

    @PostMapping("/topics")
    public Result<CommunityTopicVo> createTopic(
            @Valid @RequestBody CreateCommunityTopicRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(communityService.createTopic(
                SecurityUtils.currentUserId(),
                SecurityUtils.isAdmin(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PatchMapping("/topics/{topicId}")
    public Result<CommunityTopicVo> updateTopic(
            @PathVariable String topicId,
            @Valid @RequestBody UpdateCommunityTopicRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(communityService.updateTopic(
                SecurityUtils.currentUserId(),
                SecurityUtils.isAdmin(),
                topicId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/topics/{topicId}")
    public Result<CommunityTopicDeleteVo> deleteTopic(
            @PathVariable String topicId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(communityService.deleteTopic(
                SecurityUtils.currentUserId(),
                SecurityUtils.isAdmin(),
                topicId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/tags")
    public Result<List<String>> tags(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(communityService.listTags(limit));
    }

    @GetMapping("/posts")
    public Result<CommunityPostPageVo> posts(
            @RequestParam(required = false) String topicId,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return Result.success(communityService.listPosts(topicId, q, page, size, sort));
    }

    @PostMapping("/posts")
    public Result<CommunityPostVo> createPost(
            @Valid @RequestBody CreateCommunityPostRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(communityService.createPost(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/posts/{postId}")
    public Result<CommunityPostVo> readPost(@PathVariable String postId) {
        return Result.success(communityService.readPost(postId));
    }

    @PatchMapping("/posts/{postId}")
    public Result<CommunityPostVo> updatePost(
            @PathVariable String postId,
            @Valid @RequestBody UpdateCommunityPostRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(communityService.updatePost(
                SecurityUtils.currentUserId(),
                postId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/posts/{postId}")
    public Result<CommunityPostVo> deletePost(@PathVariable String postId, HttpServletRequest httpRequest) {
        return Result.success(communityService.deletePost(
                SecurityUtils.currentUserId(),
                postId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/posts/{postId}/pin")
    public Result<CommunityPostVo> pinPost(@PathVariable String postId, HttpServletRequest httpRequest) {
        return Result.success(communityService.pinPost(
                SecurityUtils.currentUserId(),
                SecurityUtils.isAdmin(),
                postId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/posts/{postId}/lock")
    public Result<CommunityPostVo> lockPost(@PathVariable String postId, HttpServletRequest httpRequest) {
        return Result.success(communityService.lockPost(
                SecurityUtils.currentUserId(),
                SecurityUtils.isAdmin(),
                postId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/posts/{postId}/comments")
    public Result<List<CommunityCommentVo>> comments(@PathVariable String postId) {
        return Result.success(communityService.listComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public Result<CommunityCommentVo> createComment(
            @PathVariable String postId,
            @Valid @RequestBody CreateCommunityCommentRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(communityService.createComment(
                SecurityUtils.currentUserId(),
                postId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<CommunityCommentVo> deleteComment(@PathVariable String commentId, HttpServletRequest httpRequest) {
        return Result.success(communityService.deleteComment(
                SecurityUtils.currentUserId(),
                commentId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/posts/{postId}/like")
    public Result<CommunityReactionVo> like(@PathVariable String postId) {
        return Result.success(engagementService.toggleLike(SecurityUtils.currentUserId(), postId));
    }

    @PostMapping("/posts/{postId}/bookmark")
    public Result<CommunityBookmarkVo> bookmark(@PathVariable String postId) {
        return Result.success(engagementService.toggleBookmark(SecurityUtils.currentUserId(), postId));
    }

    @PostMapping("/posts/{postId}/view")
    public Result<CommunityViewVo> view(@PathVariable String postId) {
        return Result.success(engagementService.recordView(SecurityUtils.currentUserId(), postId));
    }

    @GetMapping("/me/bookmarks")
    public Result<CommunityPostPageVo> bookmarks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(engagementService.listBookmarks(SecurityUtils.currentUserId(), page, size));
    }

    @PostMapping("/reports")
    public Result<CommunityReportVo> report(
            @Valid @RequestBody CreateCommunityReportRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(moderationService.createReport(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/admin/reports")
    public Result<CommunityReportPageVo> reports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(moderationService.listReports(SecurityUtils.isAdmin(), status, page, size));
    }

    @PatchMapping("/admin/reports/{reportId}")
    public Result<CommunityReportVo> moderateReport(
            @PathVariable String reportId,
            @Valid @RequestBody ModerateCommunityReportRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(moderationService.moderateReport(
                SecurityUtils.currentUserId(),
                SecurityUtils.isAdmin(),
                reportId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }
}
