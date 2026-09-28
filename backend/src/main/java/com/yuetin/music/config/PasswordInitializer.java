package com.yuetin.music.config;

import com.yuetin.music.entity.User;
import com.yuetin.music.mapper.UserMapper;
import com.yuetin.music.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时把 data.sql 里的明文演示密码升级为散列值。
 *
 * <p>这样种子脚本可以保持可读（方便讲解与验收），
 * 而库里最终落的一定是不可逆的 SHA-256 散列。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordInitializer implements CommandLineRunner {

    private final UserMapper userMapper;

    @Override
    public void run(String... args) {
        List<User> users = userMapper.search(null);
        int upgraded = 0;
        for (User u : users) {
            String stored = u.getPassword();
            if (stored == null || stored.length() == 64 && isHex(stored)) {
                continue; // 已经是散列
            }
            u.setPassword(PasswordUtil.encode(stored));
            userMapper.update(u);
            upgraded++;
        }
        if (upgraded > 0) {
            log.info("已将 {} 个演示账号的明文密码升级为散列存储", upgraded);
        }
    }

    private boolean isHex(String s) {
        for (char c : s.toCharArray()) {
            if (Character.digit(c, 16) < 0) return false;
        }
        return true;
    }
}
