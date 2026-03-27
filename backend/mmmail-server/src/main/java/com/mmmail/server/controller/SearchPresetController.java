package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateSearchPresetRequest;
import com.mmmail.server.model.dto.UpdateSearchPresetRequest;
import com.mmmail.server.model.vo.SearchPresetVo;
import com.mmmail.server.service.SearchPresetService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search-presets")
public class SearchPresetController {

    private final SearchPresetService searchPresetService;

    public SearchPresetController(SearchPresetService searchPresetService) {
        this.searchPresetService = searchPresetService;
    }

    @GetMapping
    public Result<List<SearchPresetVo>> list() {
        return Result.success(searchPresetService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    public Result<SearchPresetVo> create(
            @Valid @RequestBody CreateSearchPresetRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(searchPresetService.create(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{presetId}/use")
    public Result<SearchPresetVo> use(
            @PathVariable Long presetId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(searchPresetService.use(
                SecurityUtils.currentUserId(),
                presetId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{presetId}")
    public Result<SearchPresetVo> update(
            @PathVariable Long presetId,
            @Valid @RequestBody UpdateSearchPresetRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(searchPresetService.update(
                SecurityUtils.currentUserId(),
                presetId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{presetId}/pin")
    public Result<SearchPresetVo> pin(
            @PathVariable Long presetId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(searchPresetService.pin(
                SecurityUtils.currentUserId(),
                presetId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{presetId}/unpin")
    public Result<SearchPresetVo> unpin(
            @PathVariable Long presetId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(searchPresetService.unpin(
                SecurityUtils.currentUserId(),
                presetId,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{presetId}")
    public Result<Void> delete(
            @PathVariable Long presetId,
            HttpServletRequest httpRequest
    ) {
        searchPresetService.delete(SecurityUtils.currentUserId(), presetId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }
}
