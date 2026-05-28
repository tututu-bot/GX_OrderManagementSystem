package com.example.gx_ordersystem.controller;

import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Customer;
import com.example.gx_ordersystem.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户管理控制器
 * 处理客户相关的HTTP请求，提供客户增删改查RESTful API
 *
 * 基础请求路径: /api/customer
 *
 * @RestController 注解: 标识为Spring MVC控制器，自动将返回值转换为JSON格式
 * @RequestMapping("/api/customer") 注解: 定义该控制器的根路径
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    /**
     * 客户业务逻辑层接口
     * @Autowired 注解: Spring自动注入CustomerService实现类
     */
    @Autowired
    private CustomerService customerService;

    /**
     * 新增客户
     * 请求方式: POST
     * 请求路径: /api/customer
     * 请求体: Customer对象的JSON格式
     *
     * @param customer 客户对象（包含customerName, category, contactName, phone, address, remark等字段）
     * @return Result<Boolean> 成功返回true
     *
     * 示例请求:
     * POST /api/customer
     * {
     *   "customerName": "佛山英子",
     *   "category": "批发",
     *   "contactName": "李光利",
     *   "phone": "13713381199",
     *   "address": "虎门富民布料市场11排12号",
     *   "remark": "长期合作"
     * }
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody Customer customer) {
        return Result.success(customerService.save(customer));
    }

    /**
     * 删除客户（逻辑删除，保留历史数据）
     * 请求方式: DELETE
     * 请求路径: /api/customer/{id}
     * 路径参数: id - 客户ID
     *
     * @param id 客户ID
     * @return Result<Boolean> 删除成功返回true
     *
     * 示例请求: DELETE /api/customer/1
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(customerService.removeById(id));
    }

    /**
     * 修改客户信息
     * 请求方式: PUT
     * 请求路径: /api/customer
     * 请求体: Customer对象的JSON格式（必须包含id字段）
     *
     * @param customer 客户对象（包含需要更新的字段）
     * @return Result<Boolean> 修改成功返回true
     *
     * 示例请求:
     * PUT /api/customer
     * {
     *   "id": 1,
     *   "customerName": "佛山英子改",
     *   "phone": "13713381100"
     * }
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody Customer customer) {
        return Result.success(customerService.updateById(customer));
    }

    /**
     * 根据ID查询客户
     * 请求方式: GET
     * 请求路径: /api/customer/{id}
     * 路径参数: id - 客户ID
     *
     * @param id 客户ID
     * @return Result<Customer> 客户详情对象
     *
     * 示例请求: GET /api/customer/1
     */
    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable Long id) {
        return Result.success(customerService.getById(id));
    }

    /**
     * 查询全部客户列表
     * 请求方式: GET
     * 请求路径: /api/customer
     *
     * @return Result<List<Customer>> 客户列表
     *
     * 示例请求: GET /api/customer
     */
    @GetMapping
    public Result<List<Customer>> list() {
        return Result.success(customerService.list());
    }
}
