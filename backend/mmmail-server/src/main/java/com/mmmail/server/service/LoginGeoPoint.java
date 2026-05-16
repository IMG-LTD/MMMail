package com.mmmail.server.service;

public record LoginGeoPoint(
        String ipAddress,
        String city,
        String country,
        double latitude,
        double longitude,
        String source
) {
}
