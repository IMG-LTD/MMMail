package com.mmmail.server.service;

public interface LoginGeoResolver {

    LoginGeoPoint resolve(String ipAddress);
}
