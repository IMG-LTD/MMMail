package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CommunityCommentMapper;
import com.mmmail.server.mapper.CommunityPostMapper;
import com.mmmail.server.mapper.CommunityTopicMapper;
import com.mmmail.server.model.dto.CreateCommunityCommentRequest;
import com.mmmail.server.model.dto.CreateCommunityPostRequest;
import com.mmmail.server.model.dto.CreateCommunityTopicRequest;
import com.mmmail.server.model.dto.UpdateCommunityPostRequest;
import com.mmmail.server.model.dto.UpdateCommunityTopicRequest;
import com.mmmail.server.model.entity.CommunityComment;
import com.mmmail.server.model.entity.CommunityPost;
import com.mmmail.server.model.entity.CommunityTopic;
import com.mmmail.server.model.vo.CommunityCommentVo;
import com.mmmail.server.model.vo.CommunityPostPageVo;
import com.mmmail.server.model.vo.CommunityPostVo;
import com.mmmail.server.model.vo.CommunityTopicDeleteVo;
import com.mmmail.server.model.vo.CommunityTopicVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommunityService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int DEFAULT_TOPIC_SORT_ORDER = 100;
    private static final int TAG_SCAN_LIMIT = 500;
    private static final int MAX_TAG_LIMIT = 50;
    private static final String DEFAULT_TOPIC_ID = "tp_general";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_DELETED = "deleted";

    private final CommunityTopicMapper topicMapper;
    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final AuditService auditService;
    private final CommunityReadModelAssembler assembler;

    public CommunityService(
            CommunityTopicMapper topicMapper,
            CommunityPostMapper postMapper,
            CommunityCommentMapper commentMapper,
            AuditService auditService,
            CommunityReadModelAssembler assembler
    ) {
        this.topicMapper = topicMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.auditService = auditService;
        this.assembler = assembler;
    }

    @Transactional
    public List<CommunityTopicVo> listTopics() {
        ensureDefaultTopic();
        return topicMapper.selectList(new LambdaQueryWrapper<CommunityTopic>()
                        .orderByAsc(CommunityTopic::getSortOrder)
                .orderByAsc(CommunityTopic::getTitle))
                .stream()
                .map(assembler::toTopicVo)
                .toList();
    }

    @Transactional
    public CommunityTopicVo createTopic(Long userId, boolean admin, CreateCommunityTopicRequest request, String ip) {
        requireAdmin(admin);
        LocalDateTime now = LocalDateTime.now();
        CommunityTopic topic = new CommunityTopic();
        topic.setId(id("tp"));
        topic.setSlug(requireText(request.slug(), ErrorCode.INVALID_ARGUMENT, "Topic slug is required"));
        topic.setTitle(requireText(request.title(), ErrorCode.INVALID_ARGUMENT, "Topic title is required"));
        topic.setDescription(trimToNull(request.description()));
        topic.setSortOrder(DEFAULT_TOPIC_SORT_ORDER);
        topic.setCreatedAt(now);
        topic.setUpdatedAt(now);
        topic.setDeleted(0);
        topicMapper.insert(topic);
        auditService.record(userId, "COMMUNITY_TOPIC_CREATE", "topicId=" + topic.getId(), ip);
        return assembler.toTopicVo(topic);
    }

    @Transactional
    public CommunityTopicVo updateTopic(
            Long userId,
            boolean admin,
            String topicId,
            UpdateCommunityTopicRequest request,
            String ip
    ) {
        requireAdmin(admin);
        CommunityTopic topic = loadTopic(topicId);
        applyTopicPatch(topic, request);
        topic.setUpdatedAt(LocalDateTime.now());
        topicMapper.updateById(topic);
        auditService.record(userId, "COMMUNITY_TOPIC_UPDATE", "topicId=" + topicId, ip);
        return assembler.toTopicVo(topic);
    }

    @Transactional
    public CommunityTopicDeleteVo deleteTopic(Long userId, boolean admin, String topicId, String ip) {
        requireAdmin(admin);
        CommunityTopic topic = loadTopic(topicId);
        ensureTopicCanBeDeleted(topic);
        topicMapper.update(null, new LambdaUpdateWrapper<CommunityTopic>()
                .eq(CommunityTopic::getId, topicId)
                .set(CommunityTopic::getSlug, deletedTopicSlug(topic))
                .set(CommunityTopic::getDeleted, 1)
                .set(CommunityTopic::getUpdatedAt, LocalDateTime.now()));
        auditService.record(userId, "COMMUNITY_TOPIC_DELETE", "topicId=" + topicId, ip);
        return new CommunityTopicDeleteVo(topicId, true);
    }

    private String deletedTopicSlug(CommunityTopic topic) {
        return topic.getSlug() + "-deleted-" + topic.getId();
    }

    public CommunityPostPageVo listPosts(String topicId, String keyword, int page, int size, String sort) {
        IPage<CommunityPost> result = postMapper.selectPage(
                new Page<>(safePage(page), safeSize(size)),
                buildPostQuery(topicId, keyword, sort)
        );
        return assembler.toPostPage(result, safePage(page), safeSize(size));
    }

    @Transactional
    public CommunityPostVo createPost(Long userId, CreateCommunityPostRequest request, String ip) {
        CommunityTopic topic = loadTopicOrDefault(request.topicId());
        CommunityPost post = new CommunityPost();
        LocalDateTime now = LocalDateTime.now();
        post.setId(id("ps"));
        post.setAuthorUserId(userId);
        post.setTopicId(topic.getId());
        applyPostContent(post, request.title(), request.bodyMd(), request.tags());
        initializePostCounters(post);
        post.setStatus(STATUS_PUBLISHED);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        post.setDeleted(0);
        postMapper.insert(post);
        auditService.record(userId, "COMMUNITY_POST_CREATE", "postId=" + post.getId(), ip);
        return assembler.toPostVo(post);
    }

    public CommunityPostVo readPost(String postId) {
        return assembler.toPostVo(loadPost(postId));
    }

    @Transactional
    public CommunityPostVo updatePost(Long userId, String postId, UpdateCommunityPostRequest request, String ip) {
        CommunityPost post = loadPost(postId);
        requireAuthor(userId, post);
        requireUnlocked(post);
        CommunityTopic topic = loadTopicOrDefault(request.topicId());
        post.setTopicId(topic.getId());
        applyPostContent(post, request.title(), request.bodyMd(), request.tags());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        auditService.record(userId, "COMMUNITY_POST_UPDATE", "postId=" + postId, ip);
        return assembler.toPostVo(post);
    }

    @Transactional
    public CommunityPostVo deletePost(Long userId, String postId, String ip) {
        CommunityPost post = loadPost(postId);
        requireAuthor(userId, post);
        post.setStatus(STATUS_DELETED);
        post.setDeletedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        auditService.record(userId, "COMMUNITY_POST_DELETE", "postId=" + postId, ip);
        return assembler.toPostVo(post);
    }

    @Transactional
    public CommunityPostVo pinPost(Long userId, boolean admin, String postId, String ip) {
        requireAdmin(admin);
        CommunityPost post = loadPost(postId);
        post.setPinned(post.getPinned() == 1 ? 0 : 1);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        auditService.record(userId, "COMMUNITY_POST_PIN", "postId=" + postId, ip);
        return assembler.toPostVo(post);
    }

    @Transactional
    public CommunityPostVo lockPost(Long userId, boolean admin, String postId, String ip) {
        requireAdmin(admin);
        CommunityPost post = loadPost(postId);
        post.setLocked(post.getLocked() == 1 ? 0 : 1);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        auditService.record(userId, "COMMUNITY_POST_LOCK", "postId=" + postId, ip);
        return assembler.toPostVo(post);
    }

    public List<CommunityCommentVo> listComments(String postId) {
        loadPost(postId);
        List<CommunityComment> comments = commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getPostId, postId)
                .orderByAsc(CommunityComment::getCreatedAt)
                .orderByAsc(CommunityComment::getId));
        return assembler.buildCommentTree(comments);
    }

    @Transactional
    public CommunityCommentVo createComment(Long userId, String postId, CreateCommunityCommentRequest request, String ip) {
        CommunityPost post = loadPost(postId);
        requireUnlocked(post);
        CommunityComment comment = new CommunityComment();
        LocalDateTime now = LocalDateTime.now();
        comment.setId(id("cm"));
        comment.setPostId(postId);
        comment.setParentCommentId(validateParentComment(postId, request.parentCommentId()));
        comment.setAuthorUserId(userId);
        comment.setBodyMd(requireText(request.bodyMd(), ErrorCode.INVALID_ARGUMENT, "Comment body is required"));
        comment.setBodyHtml(assembler.renderMarkdown(comment.getBodyMd()));
        comment.setStatus(STATUS_PUBLISHED);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment.setDeleted(0);
        commentMapper.insert(comment);
        incrementCommentCount(post);
        auditService.record(userId, "COMMUNITY_COMMENT_CREATE", "commentId=" + comment.getId(), ip);
        return assembler.toCommentVo(comment, List.of());
    }

    @Transactional
    public CommunityCommentVo deleteComment(Long userId, String commentId, String ip) {
        CommunityComment comment = loadComment(commentId);
        if (!comment.getAuthorUserId().equals(userId)) {
            throw new BizException(ErrorCode.COMMUNITY_NOT_AUTHOR);
        }
        markCommentDeleted(comment);
        markChildCommentsDeleted(commentId);
        auditService.record(userId, "COMMUNITY_COMMENT_DELETE", "commentId=" + commentId, ip);
        return assembler.toCommentVo(comment, List.of());
    }

    public List<String> listTags(int limit) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        postMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                        .ne(CommunityPost::getStatus, STATUS_DELETED)
                        .orderByDesc(CommunityPost::getUpdatedAt)
                        .last("limit " + TAG_SCAN_LIMIT))
                .forEach(post -> assembler.readTags(post.getTagsJson()).forEach(tag -> counts.merge(tag, 1, Integer::sum)));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(1, Math.min(limit, MAX_TAG_LIMIT)))
                .map(Map.Entry::getKey)
                .toList();
    }

    private LambdaQueryWrapper<CommunityPost> buildPostQuery(String topicId, String keyword, String sort) {
        LambdaQueryWrapper<CommunityPost> query = new LambdaQueryWrapper<CommunityPost>()
                .ne(CommunityPost::getStatus, STATUS_DELETED)
                .orderByDesc(CommunityPost::getPinned);
        if (StringUtils.hasText(topicId)) {
            query.eq(CommunityPost::getTopicId, topicId.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String term = keyword.trim();
            query.and(wrapper -> wrapper.like(CommunityPost::getTitle, term).or().like(CommunityPost::getBodyMd, term));
        }
        applyPostSort(query, sort);
        return query;
    }

    private void applyPostSort(LambdaQueryWrapper<CommunityPost> query, String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            query.orderByDesc(CommunityPost::getLikeCount).orderByDesc(CommunityPost::getCommentCount);
            return;
        }
        query.orderByDesc(CommunityPost::getUpdatedAt);
    }

    private void ensureDefaultTopic() {
        if (topicMapper.selectById(DEFAULT_TOPIC_ID) != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CommunityTopic topic = new CommunityTopic();
        topic.setId(DEFAULT_TOPIC_ID);
        topic.setSlug("general");
        topic.setTitle("General");
        topic.setDescription("General discussion");
        topic.setSortOrder(0);
        topic.setCreatedAt(now);
        topic.setUpdatedAt(now);
        topic.setDeleted(0);
        topicMapper.insert(topic);
    }

    private CommunityTopic loadTopicOrDefault(String topicId) {
        ensureDefaultTopic();
        String id = StringUtils.hasText(topicId) ? topicId.trim() : DEFAULT_TOPIC_ID;
        return loadTopic(id);
    }

    private CommunityTopic loadTopic(String topicId) {
        CommunityTopic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BizException(ErrorCode.COMMUNITY_TOPIC_NOT_FOUND);
        }
        return topic;
    }

    private void applyTopicPatch(CommunityTopic topic, UpdateCommunityTopicRequest request) {
        if (request.slug() == null && request.title() == null && request.description() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Topic update requires a field");
        }
        if (request.slug() != null) {
            topic.setSlug(requireText(request.slug(), ErrorCode.INVALID_ARGUMENT, "Topic slug is required"));
        }
        if (request.title() != null) {
            topic.setTitle(requireText(request.title(), ErrorCode.INVALID_ARGUMENT, "Topic title is required"));
        }
        if (request.description() != null) {
            topic.setDescription(trimToNull(request.description()));
        }
    }

    private void ensureTopicCanBeDeleted(CommunityTopic topic) {
        if (DEFAULT_TOPIC_ID.equals(topic.getId())) {
            throw new BizException(ErrorCode.COMMUNITY_TOPIC_NOT_EMPTY);
        }
        Long activePosts = postMapper.selectCount(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getTopicId, topic.getId())
                .ne(CommunityPost::getStatus, STATUS_DELETED));
        if (activePosts > 0) {
            throw new BizException(ErrorCode.COMMUNITY_TOPIC_NOT_EMPTY);
        }
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

    private void applyPostContent(CommunityPost post, String title, String bodyMd, List<String> tags) {
        post.setTitle(requireText(title, ErrorCode.COMMUNITY_TITLE_REQUIRED, "Post title is required"));
        post.setBodyMd(requireText(bodyMd, ErrorCode.INVALID_ARGUMENT, "Post body is required"));
        post.setBodyHtml(assembler.renderMarkdown(post.getBodyMd()));
        post.setTagsJson(assembler.writeTags(assembler.normalizeTags(tags)));
    }

    private void initializePostCounters(CommunityPost post) {
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setPinned(0);
        post.setLocked(0);
    }

    private String validateParentComment(String postId, String parentCommentId) {
        if (!StringUtils.hasText(parentCommentId)) {
            return null;
        }
        CommunityComment parent = loadComment(parentCommentId);
        if (!postId.equals(parent.getPostId()) || parent.getParentCommentId() != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only one-level replies are supported");
        }
        return parent.getId();
    }

    private void incrementCommentCount(CommunityPost post) {
        post.setCommentCount(post.getCommentCount() + 1);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
    }

    private void markCommentDeleted(CommunityComment comment) {
        comment.setStatus(STATUS_DELETED);
        comment.setDeletedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(comment);
    }

    private void markChildCommentsDeleted(String commentId) {
        commentMapper.update(null, new LambdaUpdateWrapper<CommunityComment>()
                .eq(CommunityComment::getParentCommentId, commentId)
                .set(CommunityComment::getStatus, STATUS_DELETED)
                .set(CommunityComment::getDeletedAt, LocalDateTime.now())
                .set(CommunityComment::getUpdatedAt, LocalDateTime.now()));
    }

    private void requireAuthor(Long userId, CommunityPost post) {
        if (!post.getAuthorUserId().equals(userId)) {
            throw new BizException(ErrorCode.COMMUNITY_NOT_AUTHOR);
        }
    }

    private void requireAdmin(boolean admin) {
        if (!admin) {
            throw new BizException(ErrorCode.COMMUNITY_ADMIN_REQUIRED);
        }
    }

    private void requireUnlocked(CommunityPost post) {
        if (post.getLocked() == 1) {
            throw new BizException(ErrorCode.COMMUNITY_POST_LOCKED);
        }
    }

    private String requireText(String value, ErrorCode errorCode, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(errorCode, message);
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

    private String id(String prefix) {
        return prefix + "_" + IdWorker.getIdStr();
    }
}
