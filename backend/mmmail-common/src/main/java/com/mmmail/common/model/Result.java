package com.mmmail.common.model;

public record Result<T>(int code, String message, T data, long timestamp) {

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data, System.currentTimeMillis());
    }

    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
}
