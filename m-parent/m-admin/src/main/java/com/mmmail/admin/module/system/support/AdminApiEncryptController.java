package com.mmmail.admin.module.system.support;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.mmmail.base.common.controller.SupportBaseController;
import com.mmmail.base.common.domain.ResponseDTO;
import com.mmmail.base.common.domain.ValidateList;
import com.mmmail.base.constant.SwaggerTagConst;
import com.mmmail.base.module.support.apiencrypt.annotation.ApiDecrypt;
import com.mmmail.base.module.support.apiencrypt.annotation.ApiEncrypt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 * api 加密
 *
 * @Author 1024创新实验室-主任:卓大
 * @Date 2023/10/21 09:21:20
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */

@RestController
@Tag(name = SwaggerTagConst.Support.PROTECT)
public class AdminApiEncryptController extends SupportBaseController {


    @ApiDecrypt
    @PostMapping("/apiEncrypt/testRequestEncrypt")
    @Operation(summary = "测试 请求加密")
    public ResponseDTO<JweForm> testRequestEncrypt(@RequestBody @Valid JweForm form) {
        return ResponseDTO.ok(form);
    }

    @ApiEncrypt
    @PostMapping("/apiEncrypt/testResponseEncrypt")
    @Operation(summary = "测试 返回加密")
    public ResponseDTO<JweForm> testResponseEncrypt(@RequestBody @Valid JweForm form) {
        return ResponseDTO.ok(form);
    }

    @ApiDecrypt
    @ApiEncrypt
    @PostMapping("/apiEncrypt/testDecryptAndEncrypt")
    @Operation(summary = "测试 请求参数加密和解密、返回数据加密和解密")
    public ResponseDTO<JweForm> testDecryptAndEncrypt(@RequestBody @Valid JweForm form) {
        return ResponseDTO.ok(form);
    }

    @ApiDecrypt
    @ApiEncrypt
    @PostMapping("/apiEncrypt/testArray")
    @Operation(summary = "测试 数组加密和解密")
    public ResponseDTO<List<JweForm>> testArray(@RequestBody @Valid ValidateList<JweForm> list) {
        return ResponseDTO.ok(list);
    }


    @Data
    public static class JweForm {

        @NotBlank(message = "姓名 不能为空")
        String name;

        @NotNull
        @Min(value = 1)
        Integer age;

    }

}
