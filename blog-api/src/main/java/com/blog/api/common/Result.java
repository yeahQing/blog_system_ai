package com.blog.api.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结果封装类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;    // 状态码 (200: 成功, 其他: 失败)
    private String message;  // 提示信息
    private T data;          // 返回数据

    /**
     * 成功返回
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 成功返回 (无数据)
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 失败返回 (默认 500)
     */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
