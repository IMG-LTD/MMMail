package com.mmmail.base.module.support.helpdoc.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.mmmail.base.common.domain.PageParam;

/**
 * 查阅记录 查询
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-08-20 23:11:42
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class HelpDocViewRecordQueryForm extends PageParam {

    @Schema(description = "帮助文档id")
    @NotNull(message = "帮助文档id不能为空")
    private Long helpDocId;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "关键字")
    private String keywords;


}
