package com.yuetin.music.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 口令散列：SHA-256(明文 + 固定盐)，避免明文落库。
 *
 * <p>说明：这是课程项目级别的实现，用于演示「不存明文密码」。
 * 生产环境应替换为 BCrypt / SCrypt 等自带盐与工作因子的算法。</p>
 */
public final class PasswordUtil {

    private static final String SALT = "yuetin@2024";

    private PasswordUtil() {
    }

    public static String encode(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((raw + SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }

    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) return false;
        return encode(raw).equalsIgnoreCase(stored);
    }
}
