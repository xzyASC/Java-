package com.example.seckilldemo.utils;

import java.util.UUID;

/**
 * UUID工具类
 */
public class UUIDUtil {


    /**
     * 随机生成一个UUID字符串
     * @return
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
