package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CommunityCommentMapper;
import com.mmmail.server.mapper.CommunityPostMapper;
import com.mmmail.server.mapper.CommunityReportMapper;
import com.mmmail.server.model.dto.CreateCommunityReportRequest;
import com.mmmail.server.model.dto.ModerateCommunityReportRequest;
import com.mmmail.server.model.entity.CommunityComment;
import com.mmmail.server.model.entity.CommunityPost;
import com.mmmail.server.model.entity.CommunityReport;
import com.mmmail.server.model.vo.CommunityReportPageVo;
import com.mmmail.server.model.vo.CommunityReportVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class CommunityModerationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final String STATUS_HIDDEN = "hidden";
    private static final String STATUS_DELETED = "deleted";

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityReportMapper reportMapper;
    private final AuditService auditService;
    private final CommunityReadModelAssembler assembler;

    public CommunityModerationService(
            CommunityPostMapper postMapper,
            CommunityCommentMapper commentMapper,
            CommunityReportMapper reportMapper,
            AuditService auditService,
            CommunityReadModelAssembler assembler
    ) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.reportMapper = reportMapper;
        this.auditService = auditService;
        this.assembler = assembler;
    }

    @Transactional
    public CommunityReportVo createReport(Long userId, CreateCommunityReportRequest request, String ip) {
        validateReportTarget(request.targetType(), request.targetId());
        CommunityReport report = new CommunityReport();
        LocalDateTime now = LocalDateTime.now();
        report.setId(id());
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReporterUserId(userId);
        report.setReason(requireText(request.reason(), "Report reason is required"));
        report.setDetail(trimToNull(request.detail()));
        report.setStatus("pending");
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        report.setDeleted(0);
        reportMapper.insert(report);
        auditService.record(userId, "COMMUNITY_REPORT_CREATE", "reportId=" + report.getId(), ip);
        return assembler.toReportVo(report);
    }

    public CommunityReportPageVo listReports(boolean admin, String status, int page, int size) {
        requireAdmin(admin);
        IPage<CommunityReport> result = reportMapper.selectPage(
                new Page<>(safePage(page), safeSize(size)),
                buildReportQuery(status)
        );
        return new CommunityReportPageVo(
                result.getRecords().stream().map(assembler::toReportVo).toList(),
                result.getTotal(),
                safePage(page),
                safeSize(size)
        );
    }

    @Transactional
    public CommunityReportVo moderateReport(
            Long userId,
            boolean admin,
            String reportId,
            ModerateCommunityReportRequest request,
            String ip
    ) {
        requireAdmin(admin);
        CommunityReport report = loadReport(reportId);
        applyReportAction(report, request.action());
        report.setStatus("actioned");
        report.setAssigneeUserId(userId);
        report.setAction(request.action());
        report.setActionNote(trimToNull(request.actionNote()));
        report.setActionedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.updateById(report);
        auditService.record(userId, "COMMUNITY_REPORT_ACTION", "reportId=" + reportId, ip);
        return assembler.toReportVo(report);
    }

    private LambdaQueryWrapper<CommunityReport> buildReportQuery(String status) {
        LambdaQueryWrapper<CommunityReport> query = new LambdaQueryWrapper<CommunityReport>()
                .orderByAsc(CommunityReport::getCreatedAt);
        if (StringUtils.hasText(status)) {
            query.eq(CommunityReport::getStatus, status.trim());
        }
        return query;
    }

    private void validateReportTarget(String targetType, String targetId) {
        if ("post".equals(targetType)) {
            loadPost(targetId);
            return;
        }
        if ("comment".equals(targetType)) {
            loadComment(targetId);
            return;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported community report target");
    }

    private void applyReportAction(CommunityReport report, String action) {
        switch (action) {
            case "dismiss" -> report.setStatus("dismissed");
            case "hide" -> markTargetStatus(report, STATUS_HIDDEN);
            case "delete" -> markTargetStatus(report, STATUS_DELETED);
            case "ban_user" -> markTargetStatus(report, STATUS_HIDDEN);
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported moderation action");
        }
    }

    private void markTargetStatus(CommunityReport report, String status) {
        if ("post".equals(report.getTargetType())) {
            CommunityPost post = loadPost(report.getTargetId());
            post.setStatus(status);
            post.setUpdatedAt(LocalDateTime.now());
            postMapper.updateById(post);
            return;
        }
        CommunityComment comment = loadComment(report.getTargetId());
        comment.setStatus(status);
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(comment);
    }

    private CommunityPost loadPost(String postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null || STATUS_DELETED.equals(post.getStatus())) {
            throw new BizException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
        return post;
    }

    private CommunityComment loadComment(String commentId) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private CommunityReport loadReport(String reportId) {
        CommunityReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(ErrorCode.COMMUNITY_REPORT_NOT_FOUND);
        }
        return report;
    }

    private void requireAdmin(boolean admin) {
        if (!admin) {
            throw new BizException(ErrorCode.COMMUNITY_ADMIN_REQUIRED);
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int safePage(int page) {
        return Math.max(DEFAULT_PAGE, page);
    }

    private int safeSize(int size) {
        int requestedSize = size <= 0 ? DEFAULT_SIZE : size;
        return Math.min(requestedSize, MAX_SIZE);
    }

    private String id() {
        return "rp_" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr();
    }
}
