package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.Result;
import com.yuetin.music.entity.User;
import com.yuetin.music.mapper.UserMapper;
import com.yuetin.music.util.JwtUtil;
import com.yuetin.music.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    /** 新密码强度：至少 8 位且同时含字母与数字（老账号登录不受影响，注册/重置时生效） */
    private static final Pattern STRONG_PWD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final int MAX_FAILED = 5;          // 连续失败次数上限
    private static final int LOCK_MINUTES = 10;       // 触发后的锁定时长

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
        checkStrongPassword(password);
        String pwdQuestion = str(body.get("pwdQuestion"));
        String pwdAnswer = str(body.get("pwdAnswer"));
        if (pwdQuestion == null || pwdAnswer == null) {
            throw BizException.badRequest("请设置密保问题与答案，用于找回密码");
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
        user.setPwdQuestion(pwdQuestion);
        user.setPwdAnswer(PasswordUtil.encode(pwdAnswer));
        user.setFailedCount(0);
        userMapper.insert(user);

        return Result.ok(issue(user));
    }

    /**
     * 登录：用户名 + 密码，返回 token 与用户信息。
     */
    public Result<Map<String, Object>> login(Map<String, Object> body) {
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        boolean remember = Boolean.parseBoolean(String.valueOf(body.get("remember")));
        if (username == null || password == null) {
            throw BizException.badRequest("用户名和密码不能为空");
        }

        User user = userMapper.findByUsername(username);
        if (user != null && user.getLockedUntil() != null) {
            long minutes = (user.getLockedUntil().getTime() - System.currentTimeMillis()) / 60000 + 1;
            if (minutes > 0) {
                throw BizException.badRequest("失败次数过多已锁定，约 " + minutes + " 分钟后再试");
            }
        }
        // 用户不存在与密码错误返回同一提示，避免用户名枚举
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            if (user != null) registerFailure(user);
            throw BizException.badRequest("用户名或密码错误");
        }
        if (user.getFailedCount() != null && user.getFailedCount() > 0) {
            user.setFailedCount(0);
            user.setLockedUntil(null);
            userMapper.update(user);
        }
        return Result.ok(issue(user, remember ? 30 : 7));
    }

    /** 连续失败达到上限则锁定一段时间。 */
    private void registerFailure(User user) {
        int n = (user.getFailedCount() == null ? 0 : user.getFailedCount()) + 1;
        user.setFailedCount(n);
        if (n >= MAX_FAILED) {
            user.setLockedUntil(new Date(System.currentTimeMillis() + LOCK_MINUTES * 60000L));
            user.setFailedCount(0);
        }
        userMapper.update(user);
    }

    /** 忘记密码第一步：返回该用户的密保问题（答案永不返回）。 */
    public Map<String, Object> pwdQuestion(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null || user.getPwdQuestion() == null || user.getPwdQuestion().isBlank()) {
            throw BizException.badRequest("该账号未设置密保问题，请联系管理员重置");
        }
        return Map.of("question", user.getPwdQuestion());
    }

    /** 忘记密码第二步：验答案、重置密码（顺带解除锁定）。 */
    public Result<Map<String, Object>> resetPassword(Map<String, Object> body) {
        String username = str(body.get("username"));
        String answer = str(body.get("answer"));
        String newPassword = str(body.get("newPassword"));
        checkStrongPassword(newPassword);
        User user = userMapper.findByUsername(username);
        if (user == null || user.getPwdAnswer() == null
                || answer == null || !PasswordUtil.matches(answer, user.getPwdAnswer())) {
            throw BizException.badRequest("密保答案不正确");
        }
        user.setPassword(PasswordUtil.encode(newPassword));
        user.setFailedCount(0);
        user.setLockedUntil(null);
        userMapper.update(user);
        return Result.ok(Map.of("ok", true));
    }

    /** 登录态下修改密码。 */
    public Result<Map<String, Object>> changePassword(Long uid, String oldPwd, String newPwd) {
        checkStrongPassword(newPwd);
        User user = userMapper.findById(uid);
        if (user == null) throw BizException.notFound("用户不存在");
        if (!PasswordUtil.matches(oldPwd == null ? "" : oldPwd, user.getPassword())) {
            throw BizException.badRequest("当前密码不正确");
        }
        user.setPassword(PasswordUtil.encode(newPwd));
        userMapper.update(user);
        return Result.ok(Map.of("ok", true));
    }

    private void checkStrongPassword(String password) {
        if (password == null || !STRONG_PWD.matcher(password).matches()) {
            throw BizException.badRequest("新密码需至少 8 位，且同时包含字母和数字");
        }
    }

    private Map<String, Object> issue(User user) {
        return issue(user, 7);
    }

    private Map<String, Object> issue(User user, int days) {
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.sign(user.getId(), user.getUsername(), user.getRole(), days));
        user.setPassword(null);   // 响应中不携带密码
        user.setPwdAnswer(null);  // 密保答案散列同样不下发
        data.put("user", user);
        return data;
    }

    private String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
