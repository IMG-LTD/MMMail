package com.mmmail.base.module.support.captcha;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import com.mmmail.base.common.controller.SupportBaseController;
import com.mmmail.base.common.domain.ResponseDTO;
import com.mmmail.base.constant.SwaggerTagConst;
import com.mmmail.base.module.support.captcha.domain.CaptchaVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码业务
 *
 * @Author 1024创新实验室: 胡克
 * @Date 2021-09-02 20:21:10
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.CAPTCHA)
@RestController
public class CaptchaController extends SupportBaseController {

    @Resource
    private CaptchaService captchaService;

    @Operation(summary = "获取图形验证码 @author 胡克")
    @GetMapping("/captcha")
    public ResponseDTO<CaptchaVO> generateCaptcha() {
        return ResponseDTO.ok(captchaService.generateCaptcha());
    }

}
