package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SearchIndexMapper;
import com.mmmail.server.model.dto.SearchQueryCriteria;
import com.mmmail.server.model.entity.SearchIndex;
import com.mmmail.server.model.vo.SearchFacetsVo;
import com.mmmail.server.model.vo.SearchItemVo;
import com.mmmail.server.model.vo.SearchNavigationVo;
import com.mmmail.server.model.vo.SearchResultVo;
import com.mmmail.server.model.vo.SearchSuggestionVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SearchQueryService {
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int DEFAULT_SUGGESTION_SIZE = 8;
    private static final int TITLE_WEIGHT = 5;
    private static final int BODY_WEIGHT = 1;

    private final SearchIndexMapper searchIndexMapper;
    private final SearchSnippetService snippetService;

    public SearchQueryService(SearchIndexMapper searchIndexMapper, SearchSnippetService snippetService) {
        this.searchIndexMapper = searchIndexMapper;
        this.snippetService = snippetService;
    }

    public SearchResultVo search(Long userId, SearchQueryCriteria criteria) {
        String keyword = normalizeKeyword(criteria.keyword());
        validatePage(criteria.page(), criteria.size());
        List<SearchItemVo> allItems = matchingRows(userId, criteria, keyword).stream()
                .map(row -> toRankedItem(row, keyword))
                .sorted(SearchRankedItem.ORDERING)
                .map(SearchRankedItem::item)
                .toList();
        int fromIndex = Math.min((criteria.page() - 1) * criteria.size(), allItems.size());
        int toIndex = Math.min(fromIndex + criteria.size(), allItems.size());
        return new SearchResultVo(
                allItems.size(),
                allItems.subList(fromIndex, toIndex),
                facets(allItems),
                criteria.page(),
                criteria.size()
        );
    }

    public List<SearchSuggestionVo> suggestions(Long userId, String keyword) {
        SearchQueryCriteria criteria = new SearchQueryCriteria(keyword, null, 1, DEFAULT_SUGGESTION_SIZE, null, null, null);
        return search(userId, criteria).items().stream()
                .map(this::toSuggestion)
                .toList();
    }

    public SearchFacetsVo facets(Long userId, SearchQueryCriteria criteria) {
        SearchQueryCriteria fullCriteria = new SearchQueryCriteria(
                criteria.keyword(), criteria.types(), 1, Integer.MAX_VALUE, criteria.from(), criteria.to(), criteria.orgId()
        );
        return search(userId, fullCriteria).facets();
    }

    private List<SearchIndex> matchingRows(Long userId, SearchQueryCriteria criteria, String keyword) {
        LambdaQueryWrapper<SearchIndex> wrapper = baseWrapper(userId, criteria, keyword);
        return searchIndexMapper.selectList(wrapper).stream()
                .filter(row -> isVisible(row, userId))
                .filter(row -> contains(row, keyword))
                .toList();
    }

    private LambdaQueryWrapper<SearchIndex> baseWrapper(
            Long userId,
            SearchQueryCriteria criteria,
            String keyword
    ) {
        LambdaQueryWrapper<SearchIndex> wrapper = new LambdaQueryWrapper<SearchIndex>()
                .eq(SearchIndex::getDeleted, 0)
                .in(SearchIndex::getModuleType, SearchModules.parse(criteria.types()))
                .and(w -> w.like(SearchIndex::getTitle, keyword).or().like(SearchIndex::getBody, keyword))
                .and(w -> visibleSql(w, userId))
                .orderByDesc(SearchIndex::getUpdatedAt);
        applyBounds(wrapper, criteria);
        return wrapper;
    }

    private void visibleSql(LambdaQueryWrapper<SearchIndex> wrapper, Long userId) {
        wrapper.isNull(SearchIndex::getOwnerUserId)
                .or()
                .eq(SearchIndex::getOwnerUserId, userId)
                .or()
                .like(SearchIndex::getAclUserIds, SearchModules.aclToken(userId));
    }

    private void applyBounds(LambdaQueryWrapper<SearchIndex> wrapper, SearchQueryCriteria criteria) {
        if (criteria.from() != null) wrapper.ge(SearchIndex::getUpdatedAt, criteria.from());
        if (criteria.to() != null) wrapper.le(SearchIndex::getUpdatedAt, criteria.to());
        if (criteria.orgId() != null) {
            wrapper.and(w -> w.isNull(SearchIndex::getOrgId).or().eq(SearchIndex::getOrgId, criteria.orgId()));
        }
    }

    private SearchRankedItem toRankedItem(SearchIndex row, String keyword) {
        double score = score(row, keyword);
        SearchItemVo item = new SearchItemVo(
                row.getModuleType(),
                row.getResourceId(),
                nullToEmpty(row.getTitle()),
                snippetService.create(row.getTitle(), row.getBody(), keyword),
                score,
                row.getUpdatedAt(),
                new SearchNavigationVo(row.getModuleType(), row.getRoutePath())
        );
        return new SearchRankedItem(item, score, safeUpdatedAt(row.getUpdatedAt()));
    }

    private SearchFacetsVo facets(List<SearchItemVo> items) {
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (SearchItemVo item : items) {
            byType.merge(item.moduleType(), 1, Integer::sum);
        }
        return new SearchFacetsVo(byType);
    }

    private SearchSuggestionVo toSuggestion(SearchItemVo item) {
        return new SearchSuggestionVo(
                item.moduleType(),
                item.resourceId(),
                item.title(),
                item.navigation().path()
        );
    }

    private boolean isVisible(SearchIndex row, Long userId) {
        return row.getOwnerUserId() == null
                || userId.equals(row.getOwnerUserId())
                || nullToEmpty(row.getAclUserIds()).contains(SearchModules.aclToken(userId));
    }

    private boolean contains(SearchIndex row, String keyword) {
        String text = (nullToEmpty(row.getTitle()) + " " + nullToEmpty(row.getBody())).toLowerCase(Locale.ROOT);
        return text.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private double score(SearchIndex row, String keyword) {
        String needle = keyword.toLowerCase(Locale.ROOT);
        int titleScore = nullToEmpty(row.getTitle()).toLowerCase(Locale.ROOT).contains(needle) ? TITLE_WEIGHT : 0;
        int bodyScore = nullToEmpty(row.getBody()).toLowerCase(Locale.ROOT).contains(needle) ? BODY_WEIGHT : 0;
        return titleScore + bodyScore;
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword) || keyword.trim().length() < MIN_QUERY_LENGTH) {
            throw new BizException(ErrorCode.SEARCH_QUERY_TOO_SHORT);
        }
        return keyword.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Search pagination is invalid");
        }
    }

    private LocalDateTime safeUpdatedAt(LocalDateTime value) {
        return value == null ? LocalDateTime.MIN : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record SearchRankedItem(SearchItemVo item, double score, LocalDateTime updatedAt) {
        private static final Comparator<SearchRankedItem> ORDERING = Comparator
                .comparingDouble(SearchRankedItem::score)
                .thenComparing(SearchRankedItem::updatedAt)
                .reversed();
    }
}
