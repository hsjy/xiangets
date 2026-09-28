package com.yuetin.music.common;

import lombok.Data;

/**
 * 从 JWT 中解析出的当前登录用户，由 AuthInterceptor 注入 ThreadLocal。
 */
@Data
public class CurrentUser {
    private Long id;
    private String username;
    private String role;

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
