package com.mmmail.admin.module.system.support;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.mmmail.base.common.controller.SupportBaseController;
import com.mmmail.base.common.domain.PageResult;
import com.mmmail.base.common.domain.ResponseDTO;
import com.mmmail.base.common.domain.ValidateList;
import com.mmmail.base.constant.SwaggerTagConst;
import com.mmmail.base.module.support.config.ConfigKeyEnum;
import com.mmmail.base.module.support.config.ConfigService;
import com.mmmail.base.module.support.securityprotect.domain.Level3ProtectConfigForm;
import com.mmmail.base.module.support.securityprotect.domain.LoginFailQueryForm;
import com.mmmail.base.module.support.securityprotect.domain.LoginFailVO;
import com.mmmail.base.module.support.securityprotect.service.Level3ProtectConfigService;
import com.mmmail.base.module.support.securityprotect.service.SecurityLoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网络安全
 *
 * @Author 1024创新实验室-主任:卓大
 * @Date 2023/10/17 19:07:27
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */

@RestController
@Tag(name = SwaggerTagConst.Support.PROTECT)
public class AdminProtectController extends SupportBaseController {

    @Resource
    private SecurityLoginService securityLoginService;

    @Resource
    private Level3ProtectConfigService level3ProtectConfigService;

    @Resource
    private ConfigService configService;


    @Operation(summary = "分页查询 @author 1024创新实验室-主任-卓大")
    @PostMapping("/protect/loginFail/queryPage")
    public ResponseDTO<PageResult<LoginFailVO>> queryPage(@RequestBody @Valid LoginFailQueryForm queryForm) {
        return ResponseDTO.ok(securityLoginService.queryPage(queryForm));
    }


    @Operation(summary = "批量删除 @author 1024创新实验室-主任-卓大")
    @PostMapping("/protect/loginFail/batchDelete")
    public ResponseDTO<String> batchDelete(@RequestBody ValidateList<Long> idList) {
        return securityLoginService.batchDelete(idList);
    }

    @Operation(summary = "更新三级等保配置 @author 1024创新实验室-主任-卓大")
    @PostMapping("/protect/level3protect/updateConfig")
    public ResponseDTO<String> updateConfig(@RequestBody @Valid Level3ProtectConfigForm configForm) {
        return level3ProtectConfigService.updateLevel3Config(configForm);
    }

    @Operation(summary = "查询 三级等保配置 @author 1024创新实验室-主任-卓大")
    @GetMapping("/protect/level3protect/getConfig")
    public ResponseDTO<String> getConfig() {
        return ResponseDTO.ok(configService.getConfigValue(ConfigKeyEnum.LEVEL3_PROTECT_CONFIG));
    }
}
