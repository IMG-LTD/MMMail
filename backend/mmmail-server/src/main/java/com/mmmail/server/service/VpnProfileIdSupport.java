package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

final class VpnProfileIdSupport {

    private VpnProfileIdSupport() {
    }

    static Long parseNullable(String rawValue, String invalidMessage) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return Long.valueOf(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, invalidMessage);
        }
    }
}
