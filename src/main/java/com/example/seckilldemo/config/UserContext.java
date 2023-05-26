package com.example.seckilldemo.config;

import com.example.seckilldemo.entity.TUser;

/**
 */
public class UserContext {


    private static ThreadLocal<TUser> userThreadLocal = new ThreadLocal<>();

    public static void setUser(TUser tUser) {
        userThreadLocal.set(tUser);
    }

    public static TUser getUser() {
        return userThreadLocal.get();
    }
}
