package com.mmmail.admin.module.business.category.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 类目 更新
 *
 * @Author 1024创新实验室: 胡克
 * @Date 2021/08/05 21:26:58
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class CategoryUpdateForm extends CategoryAddForm {

    @Schema(description = "类目id")
    @NotNull(message = "类目id不能为空")
    private Long categoryId;
}
