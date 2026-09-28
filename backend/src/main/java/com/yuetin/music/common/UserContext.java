package com.yuetin.music.common;

/**
 * 当前登录用户的线程上下文。
 */
public class UserContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    /** 取当前用户 ID，未登录返回 null（用于「可匿名但登录后更完整」的接口）。 */
    public static Long id() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
