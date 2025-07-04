package com.mmmail.admin.module.system.datascope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import com.mmmail.admin.constant.AdminSwaggerTagConst;
import com.mmmail.admin.module.system.datascope.domain.DataScopeAndViewTypeVO;
import com.mmmail.admin.module.system.datascope.service.DataScopeService;
import com.mmmail.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 查询支持的数据范围类型
 *
 * @Author 1024创新实验室: 罗伊
 * @Date 2022-03-18 20:59:17
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_DATA_SCOPE)
public class DataScopeController {

    @Resource
    private DataScopeService dataScopeService;

    @Operation(summary = "获取当前系统所配置的所有数据范围 @author 罗伊")
    @GetMapping("/dataScope/list")
    public ResponseDTO<List<DataScopeAndViewTypeVO>> dataScopeList() {
        return dataScopeService.dataScopeList();
    }


}
