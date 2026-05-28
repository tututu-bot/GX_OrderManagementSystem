package com.example.gx_ordersystem.common;

import lombok.Data;

/**
 * 统一响应结果封装类
 * 用于Controller层返回标准化的JSON响应数据
 *
 * @param <T> 响应数据的泛型类型
 */
@Data
public class Result<T> {

    /**
     * 响应状态码
     * 200-成功，500-系统错误，其他为业务自定义码
     */
    private Integer code;

    /**
     * 响应消息
     * 成功时返回"success"，失败时返回具体错误描述
     */
    private String message;

    /**
     * 响应数据体
     * 成功时携带具体业务数据，失败时为null
     */
    private T data;

    /**
     * 成功响应（携带数据）
     *
     * @param data 响应数据对象
     * @param <T>  数据类型
     * @return 封装后的成功响应对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 封装后的成功响应对象，data为null
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 错误响应（默认500状态码）
     *
     * @param message 错误描述信息
     * @param <T>     数据类型
     * @return 封装后的错误响应对象
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /**
     * 错误响应（自定义状态码）
     *
     * @param code    自定义错误码
     * @param message 错误描述信息
     * @param <T>     数据类型
     * @return 封装后的错误响应对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
