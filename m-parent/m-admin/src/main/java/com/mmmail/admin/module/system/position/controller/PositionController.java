package com.mmmail.admin.module.system.position.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.mmmail.admin.constant.AdminSwaggerTagConst;
import com.mmmail.admin.module.system.position.domain.form.PositionAddForm;
import com.mmmail.admin.module.system.position.domain.form.PositionQueryForm;
import com.mmmail.admin.module.system.position.domain.form.PositionUpdateForm;
import com.mmmail.admin.module.system.position.domain.vo.PositionVO;
import com.mmmail.admin.module.system.position.service.PositionService;
import com.mmmail.base.common.domain.PageResult;
import com.mmmail.base.common.domain.ResponseDTO;
import com.mmmail.base.common.domain.ValidateList;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 职务表 Controller
 *
 * @Author kaiyun
 * @Date 2024-06-23 23:31:38
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */

@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_POSITION)
public class PositionController {

    @Resource
    private PositionService positionService;

    @Operation(summary = "分页查询 @author kaiyun")
    @PostMapping("/position/queryPage")
    public ResponseDTO<PageResult<PositionVO>> queryPage(@RequestBody @Valid PositionQueryForm queryForm) {
        return ResponseDTO.ok(positionService.queryPage(queryForm));
    }

    @Operation(summary = "添加 @author kaiyun")
    @PostMapping("/position/add")
    public ResponseDTO<String> add(@RequestBody @Valid PositionAddForm addForm) {
        return positionService.add(addForm);
    }

    @Operation(summary = "更新 @author kaiyun")
    @PostMapping("/position/update")
    public ResponseDTO<String> update(@RequestBody @Valid PositionUpdateForm updateForm) {
        return positionService.update(updateForm);
    }

    @Operation(summary = "批量删除 @author kaiyun")
    @PostMapping("/position/batchDelete")
    public ResponseDTO<String> batchDelete(@RequestBody ValidateList<Long> idList) {
        return positionService.batchDelete(idList);
    }

    @Operation(summary = "单个删除 @author kaiyun")
    @GetMapping("/position/delete/{positionId}")
    public ResponseDTO<String> batchDelete(@PathVariable Long positionId) {
        return positionService.delete(positionId);
    }


    @Operation(summary = "不分页查询 @author kaiyun")
    @GetMapping("/position/queryList")
    public ResponseDTO<List<PositionVO>> queryList() {
        return ResponseDTO.ok(positionService.queryList());
    }
}
