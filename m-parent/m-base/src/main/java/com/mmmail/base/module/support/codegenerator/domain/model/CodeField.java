package com.mmmail.base.module.support.codegenerator.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代码生成 基础字段 模型
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-06-30 22:15:38
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */

@Data
public class CodeField {

    @Schema(description = "列")
    @NotBlank(message = " 2.字段列表 列名 不能为空")
    private String columnName;

    @Schema(description = "列备注")
    private String columnComment;

    @Schema(description = "字段名词")
    @NotBlank(message = "2.字段列表 字段名词 不能为空")
    private String label;

    @Schema(description = "字段命名")
    @NotBlank(message = "2.字段列表 字段命名 不能为空")
    private String fieldName;

    @Schema(description = "java类型")
    @NotBlank(message = "2.字段列表 java类型 不能为空")
    private String javaType;

    @Schema(description = "js类型")
    @NotBlank(message = "2.字段列表 js类型 不能为空")
    private String jsType;

    @Schema(description = "字典key")
    private String dict;

    @Schema(description = "枚举名称")
    private String enumName;

    @Schema(description = "主键")
    @NotNull(message = "2.字段列表 主键 不能为空")
    private Boolean primaryKeyFlag;

    @Schema(description = "自增")
    @NotNull(message = "2.字段列表 自增 不能为空")
    private Boolean autoIncreaseFlag;

}
