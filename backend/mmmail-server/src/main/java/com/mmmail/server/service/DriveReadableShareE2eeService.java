package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.vo.DriveShareReadableE2eeVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DriveReadableShareE2eeService {

    public static final int DISABLED_FLAG = 0;
    public static final int ENABLED_FLAG = 1;
    public static final String PASSWORD_ALGORITHM = "openpgp-password";

    public ReadableShareE2eeMetadata resolveCreate(String algorithm) {
        String normalized = requireText(algorithm, "Drive readable-share E2EE algorithm is required");
        if (!PASSWORD_ALGORITHM.equalsIgnoreCase(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive readable-share E2EE algorithm is unsupported");
        }
        return new ReadableShareE2eeMetadata(ENABLED_FLAG, PASSWORD_ALGORITHM);
    }

    public DriveShareReadableE2eeVo toVo(Integer enabled, String algorithm) {
        if (!isEnabled(enabled)) {
            return null;
        }
        return new DriveShareReadableE2eeVo(true, requireText(algorithm, "Stored Drive readable-share E2EE algorithm is required"));
    }

    public boolean isEnabled(Integer enabled) {
        return enabled != null && enabled == ENABLED_FLAG;
    }

    public ReadableShareE2eeMetadata disabled() {
        return new ReadableShareE2eeMetadata(DISABLED_FLAG, null);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    public record ReadableShareE2eeMetadata(
            int flag,
            String algorithm
    ) {
    }
}
