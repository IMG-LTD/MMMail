package com.mmmail.admin.module.system.employee.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码所需参数
 *
 * @Author 1024创新实验室: 开云
 * @Date 2021-12-20 21:06:49
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class EmployeeUpdatePasswordForm {

    @Schema(hidden = true)
    private Long employeeId;

    @Schema(description = "原密码")
    @NotBlank(message = "原密码不能为空哦")
    private String oldPassword;

    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空哦")
    private String newPassword;
}
