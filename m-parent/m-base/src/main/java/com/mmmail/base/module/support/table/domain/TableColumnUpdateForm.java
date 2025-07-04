package com.mmmail.base.module.support.table.domain;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 自定义表格列
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-08-12 22:52:21
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class TableColumnUpdateForm {

    @NotNull(message = "表id不能为空")
    private Integer tableId;

    @NotEmpty(message = "请上传列")
    private List<TableColumnItemForm> columnList;

}
