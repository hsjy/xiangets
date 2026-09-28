package com.yuetin.music.controller;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.Result;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.entity.User;
import com.yuetin.music.mapper.UserMapper;
import com.yuetin.music.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        return authService.register(body);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> body) {
        return authService.login(body);
    }

    /** 取当前登录用户信息，用于刷新页面后恢复登录态。 */
    @GetMapping("/me")
    public Result<User> me() {
        Long uid = UserContext.id();
        if (uid == null) throw BizException.unauthorized("登录已过期，请重新登录");
        User user = userMapper.findById(uid);
        if (user == null) throw BizException.unauthorized("用户不存在或已被注销");
        user.setPassword(null);
        return Result.ok(user);
    }
}
