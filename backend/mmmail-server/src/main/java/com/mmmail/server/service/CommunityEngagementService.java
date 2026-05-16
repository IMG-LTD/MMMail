package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CommunityPostBookmarkMapper;
import com.mmmail.server.mapper.CommunityPostLikeMapper;
import com.mmmail.server.mapper.CommunityPostMapper;
import com.mmmail.server.mapper.CommunityPostViewMapper;
import com.mmmail.server.model.entity.CommunityPost;
import com.mmmail.server.model.entity.CommunityPostBookmark;
import com.mmmail.server.model.entity.CommunityPostLike;
import com.mmmail.server.model.entity.CommunityPostView;
import com.mmmail.server.model.vo.CommunityBookmarkVo;
import com.mmmail.server.model.vo.CommunityPostPageVo;
import com.mmmail.server.model.vo.CommunityPostVo;
import com.mmmail.server.model.vo.CommunityReactionVo;
import com.mmmail.server.model.vo.CommunityViewVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommunityEngagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int VIEW_DEDUP_HOURS = 1;
    private static final String STATUS_DELETED = "deleted";

    private final CommunityPostMapper postMapper;
    private final CommunityPostLikeMapper likeMapper;
    private final CommunityPostBookmarkMapper bookmarkMapper;
    private final CommunityPostViewMapper viewMapper;
    private final CommunityReadModelAssembler assembler;

    public CommunityEngagementService(
            CommunityPostMapper postMapper,
            CommunityPostLikeMapper likeMapper,
            CommunityPostBookmarkMapper bookmarkMapper,
            CommunityPostViewMapper viewMapper,
            CommunityReadModelAssembler assembler
    ) {
        this.postMapper = postMapper;
        this.likeMapper = likeMapper;
        this.bookmarkMapper = bookmarkMapper;
        this.viewMapper = viewMapper;
        this.assembler = assembler;
    }

    public CommunityPostPageVo listBookmarks(Long userId, int page, int size) {
        IPage<CommunityPostBookmark> bookmarks = bookmarkMapper.selectPage(
                new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<CommunityPostBookmark>()
                        .eq(CommunityPostBookmark::getUserId, userId)
                        .eq(CommunityPostBookmark::getDeleted, 0)
                        .orderByDesc(CommunityPostBookmark::getCreatedAt)
        );
        List<String> postIds = bookmarks.getRecords().stream().map(CommunityPostBookmark::getPostId).toList();
        List<CommunityPostVo> items = bookmarkedPosts(postIds);
        return new CommunityPostPageVo(items, bookmarks.getTotal(), safePage(page), safeSize(size));
    }

    @Transactional
    public CommunityReactionVo toggleLike(Long userId, String postId) {
        CommunityPost post = loadPost(postId);
        CommunityPostLike like = findLike(userId, postId);
        boolean liked = like == null || like.getDeleted() == 1;
        upsertLike(userId, postId, like, liked);
        post.setLikeCount(Math.max(0, post.getLikeCount() + (liked ? 1 : -1)));
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        return new CommunityReactionVo(liked, post.getLikeCount());
    }

    @Transactional
    public CommunityBookmarkVo toggleBookmark(Long userId, String postId) {
        loadPost(postId);
        CommunityPostBookmark bookmark = findBookmark(userId, postId);
        boolean bookmarked = bookmark == null || bookmark.getDeleted() == 1;
        upsertBookmark(userId, postId, bookmark, bookmarked);
        return new CommunityBookmarkVo(bookmarked);
    }

    @Transactional
    public CommunityViewVo recordView(Long userId, String postId) {
        CommunityPost post = loadPost(postId);
        LocalDateTime now = LocalDateTime.now();
        CommunityPostView view = findView(userId, postId);
        if (isRecentView(view, now)) {
            return new CommunityViewVo(post.getViewCount());
        }
        upsertView(userId, postId, view, now);
        post.setViewCount(post.getViewCount() + 1);
        post.setUpdatedAt(now);
        postMapper.updateById(post);
        return new CommunityViewVo(post.getViewCount());
    }

    private List<CommunityPostVo> bookmarkedPosts(List<String> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        return postMapper.selectBatchIds(postIds).stream()
                .filter(post -> !STATUS_DELETED.equals(post.getStatus()))
                .map(assembler::toPostVo)
                .toList();
    }

    private CommunityPost loadPost(String postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null || STATUS_DELETED.equals(post.getStatus())) {
            throw new BizException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
        return post;
    }

    private CommunityPostLike findLike(Long userId, String postId) {
        return likeMapper.selectOne(new LambdaQueryWrapper<CommunityPostLike>()
                .eq(CommunityPostLike::getUserId, userId)
                .eq(CommunityPostLike::getPostId, postId));
    }

    private void upsertLike(Long userId, String postId, CommunityPostLike like, boolean active) {
        if (like == null) {
            CommunityPostLike entity = new CommunityPostLike();
            entity.setUserId(userId);
            entity.setPostId(postId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setDeleted(active ? 0 : 1);
            likeMapper.insert(entity);
            return;
        }
        like.setDeleted(active ? 0 : 1);
        likeMapper.updateById(like);
    }

    private CommunityPostBookmark findBookmark(Long userId, String postId) {
        return bookmarkMapper.selectOne(new LambdaQueryWrapper<CommunityPostBookmark>()
                .eq(CommunityPostBookmark::getUserId, userId)
                .eq(CommunityPostBookmark::getPostId, postId));
    }

    private void upsertBookmark(Long userId, String postId, CommunityPostBookmark bookmark, boolean active) {
        if (bookmark == null) {
            CommunityPostBookmark entity = new CommunityPostBookmark();
            entity.setUserId(userId);
            entity.setPostId(postId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setDeleted(active ? 0 : 1);
            bookmarkMapper.insert(entity);
            return;
        }
        bookmark.setDeleted(active ? 0 : 1);
        bookmarkMapper.updateById(bookmark);
    }

    private CommunityPostView findView(Long userId, String postId) {
        return viewMapper.selectOne(new LambdaQueryWrapper<CommunityPostView>()
                .eq(CommunityPostView::getUserId, userId)
                .eq(CommunityPostView::getPostId, postId));
    }

    private boolean isRecentView(CommunityPostView view, LocalDateTime now) {
        return view != null && view.getLastViewedAt().isAfter(now.minusHours(VIEW_DEDUP_HOURS));
    }

    private void upsertView(Long userId, String postId, CommunityPostView view, LocalDateTime now) {
        if (view == null) {
            CommunityPostView entity = new CommunityPostView();
            entity.setUserId(userId);
            entity.setPostId(postId);
            entity.setLastViewedAt(now);
            entity.setViewCount(1);
            viewMapper.insert(entity);
            return;
        }
        view.setLastViewedAt(now);
        view.setViewCount(view.getViewCount() + 1);
        viewMapper.updateById(view);
    }

    private int safePage(int page) {
        return Math.max(DEFAULT_PAGE, page);
    }

    private int safeSize(int size) {
        int requestedSize = size <= 0 ? DEFAULT_SIZE : size;
        return Math.min(requestedSize, MAX_SIZE);
    }
}
