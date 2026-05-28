package com.example.gx_ordersystem.interceptor;

import com.example.gx_ordersystem.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT Token 认证拦截器
 * 拦截需要登录才能访问的接口，验证请求头中的 Token 是否有效
 *
 * 工作原理：
 * 1. 前端登录成功后，后端返回 JWT Token
 * 2. 前端把 Token 存储在 localStorage
 * 3. 前端每次发送请求时，在请求头 Authorization 中携带 Token
 * 4. 拦截器从请求头中提取 Token，调用 JwtUtil.validateToken() 验证
 * 5. 验证通过 → 继续执行后续逻辑
 * 6. 验证失败 → 返回 401 未授权，前端跳转到登录页
 */
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 在请求到达 Controller 之前执行
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  处理器（Controller 方法）
     * @return true-放行，false-拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头中获取 Authorization
        // 前端发送请求时设置：headers: { 'Authorization': 'Bearer ' + token }
        String authHeader = request.getHeader("Authorization");

        // 如果没有 Authorization 头，返回 401
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token无效\",\"data\":null}");
            return false;
        }

        // 提取 Token（去掉 "Bearer " 前缀）
        String token = authHeader.substring(7);

        try {
            // 验证 Token 是否有效
            if (!JwtUtil.validateToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"Token已过期\",\"data\":null}");
                return false;
            }

            // Token 验证通过，将用户ID存入 request 属性，供后续使用
            Long userId = JwtUtil.getUserId(token);
            request.setAttribute("userId", userId);

            return true; // 放行

        } catch (JwtException e) {
            // Token 解析失败（格式错误、被篡改等）
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效\",\"data\":null}");
            return false;
        }
    }
}
