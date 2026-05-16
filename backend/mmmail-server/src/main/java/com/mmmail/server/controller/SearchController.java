package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.SearchQueryParams;
import com.mmmail.server.model.vo.SearchFacetsVo;
import com.mmmail.server.model.vo.SearchReindexJobVo;
import com.mmmail.server.model.vo.SearchResultVo;
import com.mmmail.server.model.vo.SearchSuggestionVo;
import com.mmmail.server.service.SearchQueryService;
import com.mmmail.server.service.SearchReindexService;
import com.mmmail.server.security.RequireRole;
import com.mmmail.server.util.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final SearchQueryService queryService;
    private final SearchReindexService reindexService;

    public SearchController(SearchQueryService queryService, SearchReindexService reindexService) {
        this.queryService = queryService;
        this.reindexService = reindexService;
    }

    @GetMapping
    public Result<SearchResultVo> search(@ModelAttribute SearchQueryParams params) {
        return Result.success(queryService.search(
                SecurityUtils.currentUserId(),
                params.toCriteria(DEFAULT_PAGE, DEFAULT_SIZE)
        ));
    }

    @GetMapping("/suggestions")
    public Result<List<SearchSuggestionVo>> suggestions(@RequestParam String q) {
        return Result.success(queryService.suggestions(SecurityUtils.currentUserId(), q));
    }

    @GetMapping("/facets")
    public Result<SearchFacetsVo> facets(@ModelAttribute SearchQueryParams params) {
        return Result.success(queryService.facets(
                SecurityUtils.currentUserId(),
                params.toCriteria(DEFAULT_PAGE, DEFAULT_SIZE)
        ));
    }

    @PostMapping("/reindex/{moduleType}")
    @RequireRole("ADMIN")
    public Result<SearchReindexJobVo> reindex(@PathVariable String moduleType) {
        return Result.success(reindexService.reindex(moduleType));
    }

    @GetMapping("/reindex/{jobId}")
    @RequireRole("ADMIN")
    public Result<SearchReindexJobVo> reindexJob(@PathVariable String jobId) {
        return Result.success(reindexService.get(jobId));
    }

}
