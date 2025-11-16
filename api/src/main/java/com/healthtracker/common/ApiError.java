package com.healthtracker.common;

import java.util.Map;

/** Error body returned by every endpoint: {"status":404,"error":"not_found","message":"..."}. */
public record ApiError(int status, String error, String message, Map<String, String> fields) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, null);
    }
}
