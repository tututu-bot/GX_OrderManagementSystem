package com.example.gx_ordersystem.controller;

import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.User;
import com.example.gx_ordersystem.service.UserService;
import com.example.gx_ordersystem.util.BCryptUtil;
import com.example.gx_ordersystem.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 处理用户相关的HTTP请求，提供用户增删改查RESTful API
 *
 * 基础请求路径: /api/user
 *
 * @RestController 注解: 标识为Spring MVC控制器，自动将返回值转换为JSON格式
 * @RequestMapping("/api/user") 注解: 定义该控制器的根路径
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    /**
     * 用户业务逻辑层接口
     * @Autowired 注解: Spring自动注入UserService实现类
     */
    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * 请求方式: POST
     * 请求路径: /api/user/login
     * 请求体: { account: "账号", password: "密码" }
     *
     * 验证账号密码后生成 JWT Token 返回给前端
     * 前端拿到 Token 后存储在 localStorage，后续请求携带在请求头中
     *
     * @param loginForm 登录表单 { account, password }
     * @return Result { token: "JWT字符串", user: { ...用户信息 } }
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        String account = loginForm.get("account");
        String password = loginForm.get("password");

        // 根据账号查询用户
        User user = userService.lambdaQuery().eq(User::getAccount, account).one();
        if (user == null) {
            return Result.error("账号不存在");
        }
        // 使用 BCrypt 验证密码（将用户输入的明文密码与数据库中的加密密码比对）
        if (!BCryptUtil.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        // 生成 JWT Token
        String token = JwtUtil.generateToken(user.getId(), user.getAccount());

        // 构造返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        return Result.success(data);
    }

    /**
     * 新增用户（注册）
     * 请求方式: POST
     * 请求路径: /api/user/register
     *
     * 注册流程：
     * 1. 接收用户提交的注册信息（包含明文密码）
     * 2. 使用 BCrypt 对明文密码进行加密
     * 3. 将加密后的密码存入数据库
     * 4. 数据库中存储的是加密后的密文，而非明文密码
     *
     * @param user 用户对象（password 字段为明文，会被加密后存储）
     * @return Result<Boolean> 成功返回true
     */
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody User user) {
        // 使用 BCrypt 加密用户输入的明文密码
        // BCrypt 会自动生成随机盐值，每次加密结果都不同，安全性高
        String encodedPassword = BCryptUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);

        return Result.success(userService.save(user));
    }

    /**
     * 删除用户
     * 请求方式: DELETE
     * 请求路径: /api/user/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(userService.removeById(id));
    }

    /**
     * 修改用户信息
     * 请求方式: PUT
     * 请求路径: /api/user
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody User user) {
        return Result.success(userService.updateById(user));
    }

    /**
     * 根据ID查询用户
     * 请求方式: GET
     * 请求路径: /api/user/{id}
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * 查询全部用户列表
     * 请求方式: GET
     * 请求路径: /api/user/list
     */
    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }
}
