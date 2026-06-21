package com.cloudShare.Response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private ErrorDetails errorDetails;
    private LocalDateTime timeStamp;

    public static <T> ApiResponse<T> failure(String message, ErrorDetails errorDetails) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = false;
        res.message = message;
        res.errorDetails = errorDetails;
        res.timeStamp = LocalDateTime.now();
        return res;

    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = true;
        res.message = message;
        res.data = data;
        res.timeStamp = LocalDateTime.now();
        return res;

    }

}
