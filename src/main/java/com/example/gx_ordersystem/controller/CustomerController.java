package com.example.gx_ordersystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Customer;
import com.example.gx_ordersystem.entity.UserCustomer;
import com.example.gx_ordersystem.service.CustomerService;
import com.example.gx_ordersystem.service.UserCustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户管理控制器
 * 处理客户相关的HTTP请求，提供客户增删改查RESTful API
 *
 * 数据隔离：每个用户只能看到自己关联的客户（通过user_customer关联表）
 * 欠款隔离：每个用户与客户之间的欠款金额独立存储在user_customer.debt_amount中
 * 支持多对多关系：不同用户可以关联同一个客户
 *
 * 基础请求路径: /api/customer
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserCustomerService userCustomerService;

    /**
     * 从请求中获取当前登录用户ID
     * AuthInterceptor在preHandle中将userId存入了request attribute
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * 获取当前用户关联的所有客户ID列表
     */
    private List<Long> getCurrentUserCustomerIds(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return userCustomerService.lambdaQuery()
                .eq(UserCustomer::getUserId, userId)
                .list()
                .stream()
                .map(UserCustomer::getCustomerId)
                .toList();
    }

    /**
     * 获取当前用户所有关联客户的欠款金额映射（customerId -> debtAmount）
     */
    private Map<Long, BigDecimal> getUserCustomerDebtMap(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return userCustomerService.lambdaQuery()
                .eq(UserCustomer::getUserId, userId)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        UserCustomer::getCustomerId,
                        uc -> uc.getDebtAmount() != null ? uc.getDebtAmount() : BigDecimal.ZERO,
                        (a, b) -> a
                ));
    }

    /**
     * 填充客户的欠款金额（将user_customer.debt_amount设置到Customer.totalDebt中返回给前端）
     */
    private void fillCustomerDebt(Customer customer, Map<Long, BigDecimal> debtMap) {
        if (customer != null && debtMap != null) {
            customer.setTotalDebt(debtMap.getOrDefault(customer.getId(), BigDecimal.ZERO));
        }
    }

    /**
     * 检查当前用户是否关联了指定客户
     */
    private boolean isCustomerLinked(Long customerId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return userCustomerService.lambdaQuery()
                .eq(UserCustomer::getUserId, userId)
                .eq(UserCustomer::getCustomerId, customerId)
                .count() > 0;
    }

    /**
     * 新增客户（或关联已有客户）
     */
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Customer customer, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Map<String, Object> resultMap = new HashMap<>();

        // 必填校验：客户名称和联系电话不能为空
        if (customer.getCustomerName() == null || customer.getCustomerName().trim().isEmpty()) {
            return Result.error("客户名称不能为空");
        }
        if (customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
            return Result.error("联系电话不能为空");
        }

        // 根据客户名称查找是否已存在（名称唯一标识客户）
        Customer existing = customerService.lambdaQuery()
                .eq(Customer::getCustomerName, customer.getCustomerName())
                .one();

        if (existing != null) {
            // 客户已存在，检查当前用户是否已关联
            boolean alreadyLinked = userCustomerService.lambdaQuery()
                    .eq(UserCustomer::getUserId, userId)
                    .eq(UserCustomer::getCustomerId, existing.getId())
                    .count() > 0;

            if (alreadyLinked) {
                return Result.error("该客户已在您的客户列表中");
            }

            // 添加关联
            UserCustomer uc = new UserCustomer();
            uc.setUserId(userId);
            uc.setCustomerId(existing.getId());
            uc.setDebtAmount(BigDecimal.ZERO);
            userCustomerService.save(uc);

            resultMap.put("customerId", existing.getId());
            resultMap.put("action", "LINKED");
            return Result.success(resultMap, "已成功关联已有客户");
        }

        // 客户不存在，创建新客户
        customerService.save(customer);

        // 添加当前用户关联（欠款初始为0）
        UserCustomer uc = new UserCustomer();
        uc.setUserId(userId);
        uc.setCustomerId(customer.getId());
        uc.setDebtAmount(BigDecimal.ZERO);
        userCustomerService.save(uc);

        resultMap.put("customerId", customer.getId());
        resultMap.put("action", "CREATED");
        return Result.success(resultMap, "客户创建成功");
    }

    /**
     * 删除客户（实际是取消当前用户与客户的关联）
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        if (!isCustomerLinked(id, request)) {
            return Result.error("无权操作该客户");
        }

        // 删除当前用户的关联
        userCustomerService.lambdaUpdate()
                .eq(UserCustomer::getUserId, userId)
                .eq(UserCustomer::getCustomerId, id)
                .remove();

        // 检查是否还有其他用户关联该客户
        long otherLinks = userCustomerService.lambdaQuery()
                .eq(UserCustomer::getCustomerId, id)
                .count();

        // 如果没有其他关联，删除客户本身
        if (otherLinks == 0) {
            customerService.removeById(id);
        }

        return Result.success(true);
    }

    /**
     * 修改客户信息（只能修改自己关联的客户）
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody Customer customer, HttpServletRequest request) {
        if (!isCustomerLinked(customer.getId(), request)) {
            return Result.error("无权修改该客户");
        }

        return Result.success(customerService.updateById(customer));
    }

    /**
     * 根据ID查询客户（只能查看自己关联的客户）
     */
    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable Long id, HttpServletRequest request) {
        if (!isCustomerLinked(id, request)) {
            return Result.error("无权查看该客户");
        }

        Customer customer = customerService.getById(id);
        if (customer == null) {
            return Result.error("客户不存在");
        }

        // 填充用户级别的欠款金额
        Map<Long, BigDecimal> debtMap = getUserCustomerDebtMap(request);
        fillCustomerDebt(customer, debtMap);

        return Result.success(customer);
    }

    /**
     * 查询当前用户关联的全部客户列表
     */
    @GetMapping
    public Result<List<Customer>> list(HttpServletRequest request) {
        List<Long> customerIds = getCurrentUserCustomerIds(request);
        if (customerIds.isEmpty()) {
            return Result.success(List.of());
        }

        Map<Long, BigDecimal> debtMap = getUserCustomerDebtMap(request);

        List<Customer> list = customerService.lambdaQuery()
                .in(Customer::getId, customerIds)
                .orderByDesc(Customer::getCreateTime)
                .list();

        // 填充每个客户的用户级别欠款金额
        list.forEach(c -> fillCustomerDebt(c, debtMap));

        return Result.success(list);
    }

    /**
     * 分页查询当前用户关联的客户列表
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {

        List<Long> customerIds = getCurrentUserCustomerIds(request);
        if (customerIds.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("list", List.of());
            emptyResult.put("total", 0);
            emptyResult.put("pages", 0);
            emptyResult.put("current", pageNum);
            emptyResult.put("size", pageSize);
            return Result.success(emptyResult);
        }

        Map<Long, BigDecimal> debtMap = getUserCustomerDebtMap(request);

        Page<Customer> page = new Page<>(pageNum, pageSize);
        IPage<Customer> resultPage = customerService.lambdaQuery()
                .in(Customer::getId, customerIds)
                .orderByDesc(Customer::getCreateTime)
                .page(page);

        // 填充每个客户的用户级别欠款金额
        resultPage.getRecords().forEach(c -> fillCustomerDebt(c, debtMap));

        Map<String, Object> result = new HashMap<>();
        result.put("list", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        return Result.success(result);
    }
}
