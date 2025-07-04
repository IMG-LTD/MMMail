package com.mmmail.base.module.support.changelog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.mmmail.base.common.controller.SupportBaseController;
import com.mmmail.base.common.domain.PageResult;
import com.mmmail.base.common.domain.ResponseDTO;
import com.mmmail.base.constant.SwaggerTagConst;
import com.mmmail.base.module.support.changelog.domain.form.ChangeLogQueryForm;
import com.mmmail.base.module.support.changelog.domain.vo.ChangeLogVO;
import com.mmmail.base.module.support.changelog.service.ChangeLogService;
import org.springframework.web.bind.annotation.*;

/**
 * 系统更新日志 Controller
 *
 * @Author 卓大
 * @Date 2022-09-26 14:53:50
 * @Copyright 1024创新实验室
 */

@RestController
@Tag(name = SwaggerTagConst.Support.CHANGE_LOG)
public class ChangeLogController extends SupportBaseController {

    @Resource
    private ChangeLogService changeLogService;

    @Operation(summary = "分页查询 @author 卓大")
    @PostMapping("/changeLog/queryPage")
    public ResponseDTO<PageResult<ChangeLogVO>> queryPage(@RequestBody @Valid ChangeLogQueryForm queryForm) {
        return ResponseDTO.ok(changeLogService.queryPage(queryForm));
    }


    @Operation(summary = "变更内容详情 @author 卓大")
    @GetMapping("/changeLog/getDetail/{changeLogId}")
    public ResponseDTO<ChangeLogVO> getDetail(@PathVariable Long changeLogId) {
        return ResponseDTO.ok(changeLogService.getById(changeLogId));
    }
}