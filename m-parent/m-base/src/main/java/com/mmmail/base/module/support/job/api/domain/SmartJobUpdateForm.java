package com.mmmail.base.module.support.job.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 定时任务 更新
 *
 * @author huke
 * @date 2024/6/17 21:30
 */
@Data
public class SmartJobUpdateForm extends SmartJobAddForm {

    @Schema(description = "任务id")
    @NotNull(message = "任务id不能为空")
    private Integer jobId;
}
