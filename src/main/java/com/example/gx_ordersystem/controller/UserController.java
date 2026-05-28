package com.example.gx_ordersystem.controller;

import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.User;
import com.example.gx_ordersystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 新增用户
     * 请求方式: POST
     * 请求路径: /api/user/register
     * 请求体: User对象的JSON格式
     *
     * @param user 用户对象（包含account, password, realName, address, phone, extraInfo等字段）
     * @return Result<Boolean> 成功返回true，失败返回false
     *
     * 示例请求:
     * POST /api/user/register
     * {
     *   "account": "zhangsan",
     *   "password": "123456",
     *   "realName": "张三",
     *   "address": "东莞市",
     *   "phone": "13800138000",
     *   "extraInfo": "管理员"
     * }
     */
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody User user) {
        return Result.success(userService.save(user));
    }

    /**
     * 删除用户
     * 请求方式: DELETE
     * 请求路径: /api/user/{id}
     * 路径参数: id - 用户ID
     *
     * @param id 用户ID
     * @return Result<Boolean> 删除成功返回true
     *
     * 示例请求: DELETE /api/user/1
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(userService.removeById(id));
    }

    /**
     * 修改用户信息
     * 请求方式: PUT
     * 请求路径: /api/user
     * 请求体: User对象的JSON格式（必须包含id字段）
     *
     * @param user 用户对象（包含需要更新的字段）
     * @return Result<Boolean> 修改成功返回true
     *
     * 示例请求:
     * PUT /api/user
     * {
     *   "id": 1,
     *   "realName": "张三改",
     *   "phone": "13900139000"
     * }
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody User user) {
        return Result.success(userService.updateById(user));
    }

    /**
     * 根据ID查询用户
     * 请求方式: GET
     * 请求路径: /api/user/{id}
     * 路径参数: id - 用户ID
     *
     * @param id 用户ID
     * @return Result<User> 用户详情对象
     *
     * 示例请求: GET /api/user/1
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * 查询全部用户列表
     * 请求方式: GET
     * 请求路径: /api/user
     *
     * @return Result<List<User>> 用户列表
     *
     * 示例请求: GET /api/user/list
     */
    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }
}
