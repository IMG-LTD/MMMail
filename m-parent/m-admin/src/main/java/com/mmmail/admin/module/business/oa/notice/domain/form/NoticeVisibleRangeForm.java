package com.mmmail.admin.module.business.oa.notice.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mmmail.admin.module.business.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import com.mmmail.base.common.swagger.SchemaEnum;
import com.mmmail.base.common.validator.enumeration.CheckEnum;

/**
 * 通知公告 可见范围数据
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-08-12 21:40:39
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeVisibleRangeForm {

    @SchemaEnum(NoticeVisibleRangeDataTypeEnum.class)
    @CheckEnum(value = NoticeVisibleRangeDataTypeEnum.class, required = true, message = "数据类型错误")
    private Integer dataType;

    @Schema(description = "员工/部门id")
    @NotNull(message = "员工/部门id不能为空")
    private Long dataId;
}
