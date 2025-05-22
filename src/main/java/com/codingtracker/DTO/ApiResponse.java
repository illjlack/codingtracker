package com.codingtracker.DTO;

import lombok.Getter;
import lombok.Setter;

/**
 * 通用 API 响应格式
 */
@Getter
@Setter
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /** 快捷成功，无数据 */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, "操作成功", null);
    }

    /** 快捷成功，有数据 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    /** 自定义成功消息和数据 */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** 业务失败 */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}