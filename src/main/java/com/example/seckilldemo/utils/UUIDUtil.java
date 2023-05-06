package com.example.seckilldemo.utils;

import java.util.UUID;

/**
 * UUID工具类
 *
 * @author: LC
 * @date 2022/3/2 5:46 下午
 * @ClassName: UUIDUtil
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
