package com.example.gx_ordersystem.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码加密工具类
 *
 * BCrypt 特点：
 * - 每次加密生成的密文都不同（内置随机盐值）
 * - 加密后的密码包含盐值，验证时无需单独存储盐值
 * - 安全性高，暴力破解成本极高
 * - Spring Security 官方推荐的加密方式
 */
public class BCryptUtil {

    /**
     * BCrypt 密码编码器（线程安全，可复用）
     */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 加密密码
     *
     * @param rawPassword 原始密码（用户输入的明文密码）
     * @return 加密后的密文（存入数据库的密码）
     *
     * 示例：
     *   输入："123456"
     *   输出："$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E"
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码
     *
     * @param rawPassword    原始密码（用户输入的明文密码）
     * @param encodedPassword 加密后的密码（从数据库取出的密文）
     * @return true-密码匹配，false-密码不匹配
     *
     * 示例：
     *   BCryptUtil.matches("123456", "$2a$10$...") → true
     *   BCryptUtil.matches("wrong", "$2a$10$...") → false
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
