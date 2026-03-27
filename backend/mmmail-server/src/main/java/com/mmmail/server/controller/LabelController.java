package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateLabelRequest;
import com.mmmail.server.model.vo.LabelVo;
import com.mmmail.server.service.LabelService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    public Result<List<LabelVo>> list() {
        return Result.success(labelService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateLabelRequest request) {
        labelService.create(SecurityUtils.currentUserId(), request.name(), request.color());
        return Result.success(null);
    }

    @DeleteMapping("/{labelId}")
    public Result<Void> delete(@PathVariable Long labelId) {
        labelService.delete(SecurityUtils.currentUserId(), labelId);
        return Result.success(null);
    }
}
