package com.example.gx_ordersystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        String account = loginForm.get("account");
        String password = loginForm.get("password");

        User user = userService.lambdaQuery().eq(User::getAccount, account).one();
        if (user == null) {
            return Result.error("账号不存在");
        }
        if (!BCryptUtil.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getAccount());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody User user) {
        String encodedPassword = BCryptUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);

        return Result.success(userService.save(user));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(userService.removeById(id));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody User user) {
        return Result.success(userService.updateById(user));
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }

    /**
     * 分页查询用户列表（MyBatis-Plus 标准分页）
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> resultPage = userService.page(page);

        Map<String, Object> result = new HashMap<>();
        result.put("list", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        return Result.success(result);
    }
}
