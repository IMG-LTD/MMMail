package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.dto.PatchV21NotificationSettingsRequest;
import com.mmmail.server.model.dto.PatchV21SecuritySettingsRequest;
import com.mmmail.server.model.dto.UpdateProfileRequest;
import com.mmmail.server.model.vo.UserPreferenceVo;
import com.mmmail.server.model.vo.UserSessionVo;
import com.mmmail.server.model.vo.V21DeviceSessionVo;
import com.mmmail.server.model.vo.V21NotificationSettingsVo;
import com.mmmail.server.model.vo.V21SecuritySettingsVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class V21SettingsRuntimeBridgeService {

    private static final String CURRENT_DEVICE = "Current device";
    private static final String ACTIVE_SESSION = "Active session";
    private static final String SECURITY_PATCH_UNSUPPORTED =
            "v2 security settings patch is not supported by the current Community runtime";
    private static final String NOTIFICATION_PATCH_UNSUPPORTED =
            "v2 notification settings patch is not persisted by the current Community runtime";

    private final UserPreferenceService userPreferenceService;
    private final AuthService authService;

    public V21SettingsRuntimeBridgeService(UserPreferenceService userPreferenceService, AuthService authService) {
        this.userPreferenceService = userPreferenceService;
        this.authService = authService;
    }

    public UserPreferenceVo profile(Long userId) {
        return userPreferenceService.getProfile(userId);
    }

    public UserPreferenceVo updateProfile(Long userId, UpdateProfileRequest request, HttpServletRequest httpRequest) {
        return userPreferenceService.updateProfile(userId, request, httpRequest.getRemoteAddr());
    }

    public V21SecuritySettingsVo security() {
        return new V21SecuritySettingsVo(false, null);
    }

    public void patchSecurity(PatchV21SecuritySettingsRequest request) {
        throw new BizException(ErrorCode.INVALID_ARGUMENT, SECURITY_PATCH_UNSUPPORTED);
    }

    public List<V21DeviceSessionVo> devices(Long userId, Long currentSessionId) {
        return authService.listSessions(userId, currentSessionId).stream()
                .map(this::toDevice)
                .toList();
    }

    public void deleteDevice(
            Long userId,
            Long currentSessionId,
            String deviceId,
            HttpServletRequest request
    ) {
        authService.revokeSession(userId, currentSessionId, parseDeviceId(deviceId), request.getRemoteAddr());
    }

    public V21NotificationSettingsVo notifications() {
        return new V21NotificationSettingsVo(true, true);
    }

    public void patchNotifications(PatchV21NotificationSettingsRequest request) {
        throw new BizException(ErrorCode.INVALID_ARGUMENT, NOTIFICATION_PATCH_UNSUPPORTED);
    }

    private V21DeviceSessionVo toDevice(UserSessionVo session) {
        return new V21DeviceSessionVo(
                session.id(),
                session.current() ? CURRENT_DEVICE : ACTIVE_SESSION,
                session.createdAt(),
                session.current()
        );
    }

    private Long parseDeviceId(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "device id is required");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "device id is invalid");
        }
    }
}
