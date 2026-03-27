package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SearchHistoryMapper;
import com.mmmail.server.model.entity.SearchHistory;
import com.mmmail.server.model.vo.SearchHistoryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SearchHistoryService {

    private static final int MAX_HISTORY_SIZE = 20;
    private static final int MAX_KEYWORD_LENGTH = 512;

    private final SearchHistoryMapper searchHistoryMapper;
    private final AuditService auditService;

    public SearchHistoryService(SearchHistoryMapper searchHistoryMapper, AuditService auditService) {
        this.searchHistoryMapper = searchHistoryMapper;
        this.auditService = auditService;
    }

    public List<SearchHistoryVo> list(Long userId) {
        return searchHistoryMapper.selectList(new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getOwnerId, userId)
                        .orderByDesc(SearchHistory::getLastUsedAt)
                        .orderByDesc(SearchHistory::getUsageCount)
                        .orderByDesc(SearchHistory::getCreatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public void recordKeyword(Long userId, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        SearchHistory existing = searchHistoryMapper.selectOne(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getOwnerId, userId)
                .eq(SearchHistory::getKeyword, normalizedKeyword));

        if (existing == null) {
            SearchHistory history = new SearchHistory();
            history.setOwnerId(userId);
            history.setKeyword(normalizedKeyword);
            history.setUsageCount(1);
            history.setLastUsedAt(now);
            history.setCreatedAt(now);
            history.setUpdatedAt(now);
            history.setDeleted(0);
            searchHistoryMapper.insert(history);
        } else {
            existing.setUsageCount((existing.getUsageCount() == null ? 0 : existing.getUsageCount()) + 1);
            existing.setLastUsedAt(now);
            existing.setUpdatedAt(now);
            searchHistoryMapper.updateById(existing);
        }

        trimHistory(userId);
    }

    @Transactional
    public void deleteItem(Long userId, Long historyId) {
        SearchHistory item = loadItem(userId, historyId);
        searchHistoryMapper.deleteById(item.getId());
    }

    @Transactional
    public void clear(Long userId, String ipAddress) {
        int deleted = searchHistoryMapper.delete(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getOwnerId, userId));
        auditService.record(userId, "SEARCH_HISTORY_CLEAR", "count=" + deleted, ipAddress);
    }

    private SearchHistory loadItem(Long userId, Long historyId) {
        SearchHistory item = searchHistoryMapper.selectOne(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getId, historyId)
                .eq(SearchHistory::getOwnerId, userId));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Search history item not found");
        }
        return item;
    }

    private void trimHistory(Long userId) {
        List<SearchHistory> list = searchHistoryMapper.selectList(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getOwnerId, userId)
                .orderByDesc(SearchHistory::getLastUsedAt)
                .orderByDesc(SearchHistory::getUsageCount)
                .orderByDesc(SearchHistory::getCreatedAt));
        if (list.size() <= MAX_HISTORY_SIZE) {
            return;
        }
        List<Long> removeIds = list.subList(MAX_HISTORY_SIZE, list.size()).stream()
                .map(SearchHistory::getId)
                .toList();
        if (!removeIds.isEmpty()) {
            searchHistoryMapper.delete(new LambdaQueryWrapper<SearchHistory>()
                    .eq(SearchHistory::getOwnerId, userId)
                    .in(SearchHistory::getId, removeIds));
        }
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Search keyword is too long");
        }
        return normalized;
    }

    private SearchHistoryVo toVo(SearchHistory history) {
        return new SearchHistoryVo(
                String.valueOf(history.getId()),
                history.getKeyword(),
                history.getUsageCount() == null ? 0 : history.getUsageCount(),
                history.getLastUsedAt()
        );
    }
}
