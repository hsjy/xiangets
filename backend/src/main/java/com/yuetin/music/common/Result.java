package com.yuetin.music.common;

import lombok.Data;

/**
 * 统一响应封装：与前端 mock 层的 { code, message, data } 契约保持一致。
 */
@Data
public class Result<T> {
    /** 200 成功；400 参数错误；401 未登录/登录过期；403 无权限；404 不存在；500 服务端错误 */
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
