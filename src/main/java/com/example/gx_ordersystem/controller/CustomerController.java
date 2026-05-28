package com.example.gx_ordersystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Customer;
import com.example.gx_ordersystem.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户管理控制器
 * 处理客户相关的HTTP请求，提供客户增删改查RESTful API
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping
    public Result<Boolean> save(@RequestBody Customer customer) {
        return Result.success(customerService.save(customer));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(customerService.removeById(id));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Customer customer) {
        return Result.success(customerService.updateById(customer));
    }

    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable Long id) {
        return Result.success(customerService.getById(id));
    }

    @GetMapping
    public Result<List<Customer>> list() {
        return Result.success(customerService.list());
    }

    /**
     * 分页查询客户列表（MyBatis-Plus 标准分页）
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<Customer> page = new Page<>(pageNum, pageSize);
        IPage<Customer> resultPage = customerService.page(page);

        Map<String, Object> result = new HashMap<>();
        result.put("list", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        return Result.success(result);
    }
}
