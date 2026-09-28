package com.yuetin.music.controller;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.CurrentUser;
import com.yuetin.music.common.Result;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.entity.User;
import com.yuetin.music.mapper.UserMapper;
import com.yuetin.music.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @GetMapping("/user/list")
    public Result<List<Map<String, Object>>> list() {
        List<User> users = userMapper.search(null);
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("name", u.getName());
            m.put("avatar", u.getAvatar());
            m.put("role", u.getRole());
            m.put("createTime", u.getCreateTime());
            out.add(m);
        }
        return Result.ok(out);
    }

    @GetMapping("/user/detail/{id}")
    public Result<User> detail(@PathVariable Long id) {
        User u = userMapper.findById(id);
        if (u == null) throw BizException.notFound("用户不存在");
        u.setPassword(null);
        return Result.ok(u);
    }

    /** 修改资料：只能改自己；改密码时需校验原密码。 */
    @PutMapping("/user/update/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        CurrentUser current = UserContext.get();
        if (current == null) throw BizException.unauthorized("登录已过期，请重新登录");
        if (!current.getId().equals(id) && !current.isAdmin()) {
            throw BizException.forbidden("只能修改自己的资料");
        }

        User u = userMapper.findById(id);
        if (u == null) throw BizException.notFound("用户不存在");

        if (body.get("name") != null) u.setName(String.valueOf(body.get("name")).trim());
        if (body.get("avatar") != null) u.setAvatar(String.valueOf(body.get("avatar")));
        if (body.get("email") != null) u.setEmail(String.valueOf(body.get("email")));
        if (body.get("phone") != null) u.setPhone(String.valueOf(body.get("phone")));
        if (body.get("birth") != null) u.setBirth(String.valueOf(body.get("birth")));
        if (body.get("intro") != null) u.setIntro(String.valueOf(body.get("intro")));
        if (body.get("sex") != null) u.setSex(Integer.valueOf(String.valueOf(body.get("sex"))));

        // 改密码：必须提供正确的旧密码
        String newPwd = body.get("password") == null ? null : String.valueOf(body.get("password"));
        if (newPwd != null && !newPwd.trim().isEmpty()) {
            String oldPwd = body.get("oldPassword") == null ? null : String.valueOf(body.get("oldPassword"));
            if (!PasswordUtil.matches(oldPwd, u.getPassword())) {
                throw BizException.badRequest("原密码不正确");
            }
            if (newPwd.length() < 6) throw BizException.badRequest("新密码至少 6 位");
            u.setPassword(PasswordUtil.encode(newPwd));
        }

        userMapper.update(u);
        u.setPassword(null);
        return Result.ok(u);
    }

    /** 注销账号：管理员或本人。关联数据由外键级联清理。 */
    @DeleteMapping("/user/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        CurrentUser current = UserContext.get();
        if (current == null) throw BizException.unauthorized("登录已过期，请重新登录");
        if (!current.getId().equals(id) && !current.isAdmin()) {
            throw BizException.forbidden("只能注销自己的账号");
        }
        if (userMapper.findById(id) == null) throw BizException.notFound("用户不存在");
        userMapper.deleteById(id);
        return Result.ok();
    }
}
