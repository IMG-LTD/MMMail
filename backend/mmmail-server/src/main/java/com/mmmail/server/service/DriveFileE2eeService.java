package com.mmmail.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.vo.DriveFileE2eeVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DriveFileE2eeService {

    public static final int E2EE_DISABLED_FLAG = 0;
    public static final int E2EE_ENABLED_FLAG = 1;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public DriveFileE2eeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DriveFileE2eeMetadata resolveUpload(Boolean enabled, String algorithm, String fingerprintsJson) {
        if (!Boolean.TRUE.equals(enabled)) {
            return DriveFileE2eeMetadata.disabled();
        }
        String normalizedAlgorithm = requireText(algorithm, "Drive file E2EE algorithm is required");
        String normalizedFingerprintsJson = requireText(
                fingerprintsJson,
                "Drive file E2EE recipient fingerprints are required"
        );
        parseFingerprints(normalizedFingerprintsJson);
        return DriveFileE2eeMetadata.enabled(normalizedAlgorithm, normalizedFingerprintsJson);
    }

    public DriveFileE2eeMetadata fromStored(Integer enabled, String algorithm, String fingerprintsJson) {
        if (!isEnabled(enabled)) {
            return DriveFileE2eeMetadata.disabled();
        }
        String normalizedAlgorithm = requireText(algorithm, "Stored Drive E2EE algorithm is required");
        String normalizedFingerprintsJson = requireText(
                fingerprintsJson,
                "Stored Drive E2EE recipient fingerprints are required"
        );
        parseFingerprints(normalizedFingerprintsJson);
        return DriveFileE2eeMetadata.enabled(normalizedAlgorithm, normalizedFingerprintsJson);
    }

    public boolean isEnabled(Integer enabled) {
        return enabled != null && enabled == E2EE_ENABLED_FLAG;
    }

    public DriveFileE2eeVo toVo(Integer enabled, String algorithm, String fingerprintsJson) {
        if (!isEnabled(enabled)) {
            return null;
        }
        return new DriveFileE2eeVo(true, algorithm, parseFingerprints(fingerprintsJson));
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    private List<String> parseFingerprints(String fingerprintsJson) {
        try {
            List<String> fingerprints = objectMapper.readValue(fingerprintsJson, STRING_LIST_TYPE);
            if (fingerprints == null || fingerprints.isEmpty()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file E2EE recipient fingerprints are required");
            }
            return fingerprints;
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file E2EE recipient fingerprints are invalid");
        }
    }

    public record DriveFileE2eeMetadata(
            int flag,
            String algorithm,
            String fingerprintsJson
    ) {

        public static DriveFileE2eeMetadata disabled() {
            return new DriveFileE2eeMetadata(E2EE_DISABLED_FLAG, null, null);
        }

        public static DriveFileE2eeMetadata enabled(String algorithm, String fingerprintsJson) {
            return new DriveFileE2eeMetadata(E2EE_ENABLED_FLAG, algorithm, fingerprintsJson);
        }

        public boolean enabled() {
            return flag == E2EE_ENABLED_FLAG;
        }
    }
}
