package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.SearchHistoryVo;
import com.mmmail.server.service.SearchHistoryService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search-history")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    public SearchHistoryController(SearchHistoryService searchHistoryService) {
        this.searchHistoryService = searchHistoryService;
    }

    @GetMapping
    public Result<List<SearchHistoryVo>> list() {
        return Result.success(searchHistoryService.list(SecurityUtils.currentUserId()));
    }

    @DeleteMapping("/{historyId}")
    public Result<Void> deleteItem(@PathVariable Long historyId) {
        searchHistoryService.deleteItem(SecurityUtils.currentUserId(), historyId);
        return Result.success(null);
    }

    @DeleteMapping
    public Result<Void> clear(HttpServletRequest httpRequest) {
        searchHistoryService.clear(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr());
        return Result.success(null);
    }
}
