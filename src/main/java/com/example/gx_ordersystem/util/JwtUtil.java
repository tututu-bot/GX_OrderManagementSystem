package com.example.gx_ordersystem.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 用于生成和验证 JWT Token
 *
 * JWT 结构：Header.Payload.Signature
 * - Header: 声明类型和加密算法
 * - Payload: 携带的数据（用户ID、账号、过期时间等）
 * - Signature: 签名，防止篡改
 */
public class JwtUtil {

    /**
     * JWT 密钥（用于签名和验证）
     * 生产环境应放在配置文件或环境变量中
     */
    private static final String SECRET = "GX_OrderSystem_SecretKey_2026_ThisIsAVeryLongSecretKeyForJWT";

    /**
     * Token 过期时间：7天（单位：毫秒）
     */
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    /**
     * 使用密钥生成 SecretKey 对象
     */
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成 JWT Token
     *
     * @param userId  用户ID
     * @param account 用户账号
     * @return JWT 字符串
     */
    public static String generateToken(Long userId, String account) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .subject(String.valueOf(userId))           // 设置主题（用户ID）
                .claim("account", account)                 // 自定义声明：账号
                .issuedAt(now)                             // 签发时间
                .expiration(expiration)                    // 过期时间
                .signWith(KEY)                             // 使用密钥签名
                .compact();                                // 生成 Token 字符串
    }

    /**
     * 解析 JWT Token，获取 Claims（载荷数据）
     *
     * @param token JWT 字符串
     * @return Claims 对象，包含用户ID、账号、过期时间等信息
     * @throws JwtException Token 无效或已过期时抛出异常
     */
    public static Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(KEY)                           // 使用密钥验证签名
                .build()
                .parseSignedClaims(token)                  // 解析 Token
                .getPayload();                             // 获取载荷数据
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT 字符串
     * @return 用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从 Token 中获取用户账号
     *
     * @param token JWT 字符串
     * @return 用户账号
     */
    public static String getAccount(String token) {
        Claims claims = parseToken(token);
        return claims.get("account", String.class);
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT 字符串
     * @return true-有效，false-无效或已过期
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
