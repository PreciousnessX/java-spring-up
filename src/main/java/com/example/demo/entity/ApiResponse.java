package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private Integer code;      // 状态码
    private String message;    // 消息
    private T data;           // 数据

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(400, message, null);
    }
}
