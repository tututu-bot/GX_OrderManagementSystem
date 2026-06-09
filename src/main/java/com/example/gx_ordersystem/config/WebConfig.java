package com.example.gx_ordersystem.config;

import com.example.gx_ordersystem.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * 配置 Spring MVC 的视图映射、静态资源访问和拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置视图控制器映射
     * 访问根路径 / 时自动跳转到登录页面
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/login.html");
    }

    /**
     * 配置拦截器
     * AuthInterceptor 会拦截所有 /api/** 请求，验证 JWT Token
     * 但以下路径会被排除（不需要登录即可访问）：
     * - /api/user/login    → 登录接口
     * - /api/user/register → 注册接口
     * - /api/user/list     → 用户列表（开发调试用，生产环境应删除）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")                          // 拦截所有 /api 开头的请求
                .excludePathPatterns(                                 // 排除以下路径（不需要Token）
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/list",
                        "/api/payment/page/**",                     // 手机扫码支付确认页
                        "/api/payment/confirm/**",                  // 手机确认支付
                        "/api/payment/qrcode/**"                    // 二维码图片
                );
    }
}
