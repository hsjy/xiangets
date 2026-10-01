package com.yuetin.music.config;

import com.yuetin.music.common.CurrentUser;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 解析 Authorization: Bearer &lt;token&gt; 并写入 UserContext。
 *
 * <p>注意与 mock 层一致：必须先剥离 "Bearer " 前缀再校验，
 * 否则会把整串 header 当成 token 导致签名比对失败。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.trim().startsWith("Bearer ")) {
            String token = header.trim().substring(7).trim();
            CurrentUser user = jwtUtil.verify(token);
            if (user != null) {
                UserContext.set(user);
            }
        }
        // 不在这里拒绝请求：公开接口允许匿名访问，
        // 需要登录的接口由 Service 层通过 UserContext.id() 判空后抛 401。
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
