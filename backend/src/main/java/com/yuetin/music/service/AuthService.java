package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.Result;
import com.yuetin.music.entity.User;
import com.yuetin.music.mapper.UserMapper;
import com.yuetin.music.util.JwtUtil;
import com.yuetin.music.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 注册：校验用户名格式与唯一性，密码加盐散列后落库。
     */
    public Result<Map<String, Object>> register(Map<String, Object> body) {
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        String name = str(body.get("name"));

        if (!USERNAME.matcher(username).matches()) {
            throw BizException.badRequest("用户名需为 3-20 位字母、数字或下划线");
        }
        if (password == null || password.length() < 6) {
            throw BizException.badRequest("密码至少 6 位");
        }
        if (userMapper.findByUsername(username) != null) {
            throw BizException.badRequest("该用户名已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setName(name == null || name.isEmpty() ? username : name);
        // 头像留空：前端以「印章头像」（用户名首字入印）作为默认形象
        user.setEmail(str(body.get("email")));
        user.setPhone(str(body.get("phone")));
        user.setSex(body.get("sex") == null ? 0 : Integer.valueOf(String.valueOf(body.get("sex"))));
        user.setBirth(str(body.get("birth")));
        user.setIntro(str(body.get("intro")));
        user.setRole("user");
        userMapper.insert(user);

        return Result.ok(issue(user));
    }

    /**
     * 登录：用户名 + 密码，返回 token 与用户信息。
     */
    public Result<Map<String, Object>> login(Map<String, Object> body) {
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        if (username == null || password == null) {
            throw BizException.badRequest("用户名和密码不能为空");
        }

        User user = userMapper.findByUsername(username);
        // 用户不存在与密码错误返回同一提示，避免用户名枚举
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            throw BizException.badRequest("用户名或密码错误");
        }
        return Result.ok(issue(user));
    }

    private Map<String, Object> issue(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.sign(user.getId(), user.getUsername(), user.getRole()));
        user.setPassword(null); // 响应中不携带密码
        data.put("user", user);
        return data;
    }

    private String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
