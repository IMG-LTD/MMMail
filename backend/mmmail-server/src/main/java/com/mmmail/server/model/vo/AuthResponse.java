package com.mmmail.server.model.vo;

public record AuthResponse(String accessToken, String refreshToken, UserProfileVo user) {
}
