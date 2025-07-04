package com.mmmail.base.module.support.message.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.mmmail.base.common.enumeration.UserTypeEnum;
import com.mmmail.base.common.swagger.SchemaEnum;
import com.mmmail.base.module.support.message.constant.MessageTypeEnum;
/**
 * 消息发送form
 *
 * @author luoyi
 * @date 2024/06/22 20:20
 */
@Data
public class MessageSendForm {

    @SchemaEnum(value = MessageTypeEnum.class, desc = "消息类型")
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;

    @SchemaEnum(value = UserTypeEnum.class, desc = "接收者类型")
    @NotNull(message = "接收者类型不能为空")
    private Integer receiverUserType;

    @Schema(description = "接收者id")
    @NotNull(message = "接收者id不能为空")
    private Long receiverUserId;

    @Schema(description = "标题")
    @NotBlank(message = "标题")
    private String title;

    @Schema(description = "内容")
    @NotBlank(message = "内容")
    private String content;

    /**
     * 相关业务id | 可选
     */
    private Object dataId;

}
