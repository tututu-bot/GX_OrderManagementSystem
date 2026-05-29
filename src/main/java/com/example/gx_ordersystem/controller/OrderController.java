package com.example.gx_ordersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Customer;
import com.example.gx_ordersystem.entity.DebtRecord;
import com.example.gx_ordersystem.entity.SaleOrder;
import com.example.gx_ordersystem.entity.SaleOrderItem;
import com.example.gx_ordersystem.entity.UserCustomer;
import com.example.gx_ordersystem.service.CustomerService;
import com.example.gx_ordersystem.service.DebtRecordService;
import com.example.gx_ordersystem.service.SaleOrderItemService;
import com.example.gx_ordersystem.service.SaleOrderService;
import com.example.gx_ordersystem.service.UserCustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单管理控制器
 * 处理销售订单相关的HTTP请求，提供订单、订单明细、欠款记录的增删改查RESTful API
 *
 * 数据隔离：每个用户只能看到自己创建的订单（通过 sale_order.user_id 实现）
 * 欠款隔离：每个用户与客户之间的欠款金额独立存储在 user_customer.debt_amount 中
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private SaleOrderService saleOrderService;
    @Autowired
    private SaleOrderItemService saleOrderItemService;
    @Autowired
    private DebtRecordService debtRecordService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private UserCustomerService userCustomerService;

    /**
     * 从请求中获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
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
     * 检查订单是否属于当前用户
     */
    private boolean isOrderOwner(SaleOrder order, HttpServletRequest request) {
        if (order == null || order.getUserId() == null) {
            return false;
        }
        return order.getUserId().equals(getCurrentUserId(request));
    }

    /**
     * 获取当前用户对指定客户的欠款金额
     */
    private BigDecimal getUserCustomerDebt(Long userId, Long customerId) {
        UserCustomer uc = userCustomerService.lambdaQuery()
                .eq(UserCustomer::getUserId, userId)
                .eq(UserCustomer::getCustomerId, customerId)
                .one();
        return (uc != null && uc.getDebtAmount() != null) ? uc.getDebtAmount() : BigDecimal.ZERO;
    }

    /**
     * 更新当前用户对指定客户的欠款金额
     */
    private void updateUserCustomerDebt(Long userId, Long customerId, BigDecimal newDebt) {
        if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
            newDebt = BigDecimal.ZERO;
        }
        userCustomerService.lambdaUpdate()
                .eq(UserCustomer::getUserId, userId)
                .eq(UserCustomer::getCustomerId, customerId)
                .set(UserCustomer::getDebtAmount, newDebt)
                .update();
    }

    /**
     * 创建欠款记录
     *
     * @param remainingAmount 剩余欠款金额，>0 表示有效，<=0 表示无效
     */
    private void createDebtRecord(Long userId, Long customerId, Long orderId, String orderNo,
                                   BigDecimal amount, BigDecimal remainingAmount, String type, String remark) {
        DebtRecord debtRecord = new DebtRecord();
        debtRecord.setUserId(userId);
        debtRecord.setCustomerId(customerId);
        debtRecord.setOrderId(orderId);
        debtRecord.setOrderNo(orderNo);
        debtRecord.setAmount(amount);
        debtRecord.setRemainingAmount(remainingAmount);
        debtRecord.setType(type);
        debtRecord.setRemark(remark);
        debtRecordService.save(debtRecord);
    }

    /**
     * 获取指定订单最新的欠款记录
     */
    private DebtRecord getLatestDebtRecord(Long orderId) {
        return debtRecordService.lambdaQuery()
                .eq(DebtRecord::getOrderId, orderId)
                .orderByDesc(DebtRecord::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    @PostMapping
    public Result<Map<String, Object>> saveOrder(@RequestBody SaleOrder order, HttpServletRequest request) {
        // 验证客户是否被当前用户关联
        if (!isCustomerLinked(order.getCustomerId(), request)) {
            return Result.error("无权为该客户创建订单");
        }

        // 设置订单创建者
        order.setUserId(getCurrentUserId(request));

        boolean saved = saleOrderService.save(order);
        if (!saved) {
            return Result.error("保存订单失败");
        }

        Long userId = getCurrentUserId(request);

        // 只要有欠款（无论哪种支付方式），都产生欠款记录并更新用户-客户欠款
        if (order.getCurrentDebt() != null
                && order.getCurrentDebt().compareTo(BigDecimal.ZERO) > 0) {
            createDebtRecord(userId, order.getCustomerId(), order.getId(), order.getOrderNo(),
                    order.getCurrentDebt(), order.getCurrentDebt(), "NEW",
                    "订单欠款，收款方式：" + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "未知"));

            BigDecimal currentDebt = getUserCustomerDebt(userId, order.getCustomerId());
            updateUserCustomerDebt(userId, order.getCustomerId(), currentDebt.add(order.getCurrentDebt()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteOrder(@PathVariable Long id) {
        return Result.success(saleOrderService.removeById(id));
    }

    /**
     * 修改订单（同时处理欠款金额变化）
     */
    @PutMapping
    public Result<Boolean> updateOrder(@RequestBody SaleOrder order, HttpServletRequest request) {
        // 获取原订单
        SaleOrder oldOrder = saleOrderService.getById(order.getId());
        if (oldOrder == null) {
            return Result.error("订单不存在");
        }

        // 权限检查：只能修改自己创建的订单
        if (!isOrderOwner(oldOrder, request)) {
            return Result.error("无权修改该订单");
        }

        // 已作废订单不允许修改
        if ("已作废".equals(oldOrder.getStatus())) {
            return Result.error("已作废的订单不能修改");
        }

        // order_no 是 UNIQUE，不能更新，置为 null 以排除
        order.setOrderNo(null);

        boolean updated = saleOrderService.updateById(order);
        if (!updated) {
            return Result.error("更新订单失败");
        }

        Long userId = getCurrentUserId(request);
        BigDecimal oldDebt = oldOrder.getCurrentDebt() != null ? oldOrder.getCurrentDebt() : BigDecimal.ZERO;
        BigDecimal newDebt = order.getCurrentDebt() != null ? order.getCurrentDebt() : BigDecimal.ZERO;
        BigDecimal diff = newDebt.subtract(oldDebt);

        // 欠款金额有变化时，更新对应欠款记录的 remaining_amount 和用户-客户欠款
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            if (oldDebt.compareTo(BigDecimal.ZERO) == 0 && newDebt.compareTo(BigDecimal.ZERO) > 0) {
                // 原先无欠款，修改后产生欠款 -> 创建新的欠款记录
                createDebtRecord(userId, order.getCustomerId(), order.getId(), order.getOrderNo(),
                        newDebt, newDebt, "NEW",
                        "订单修改，新增欠款，收款方式：" + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "未知"));
            } else if (oldDebt.compareTo(BigDecimal.ZERO) > 0 && newDebt.compareTo(BigDecimal.ZERO) == 0) {
                // 原先有欠款，修改后无欠款 -> 将最新欠款记录的 remaining_amount 设为 0（标记无效）
                DebtRecord latestRecord = getLatestDebtRecord(order.getId());
                if (latestRecord != null) {
                    latestRecord.setRemainingAmount(BigDecimal.ZERO);
                    debtRecordService.updateById(latestRecord);
                }
            } else if (oldDebt.compareTo(BigDecimal.ZERO) > 0 && newDebt.compareTo(BigDecimal.ZERO) > 0) {
                // 原先有欠款，修改后仍有欠款 -> 更新最新欠款记录的 remaining_amount
                DebtRecord latestRecord = getLatestDebtRecord(order.getId());
                if (latestRecord != null) {
                    latestRecord.setRemainingAmount(newDebt);
                    debtRecordService.updateById(latestRecord);
                }
            }

            BigDecimal currentDebt = getUserCustomerDebt(userId, order.getCustomerId());
            updateUserCustomerDebt(userId, order.getCustomerId(), currentDebt.add(diff));
        }

        return Result.success(true);
    }

    @GetMapping("/{id}")
    public Result<SaleOrder> getOrderById(@PathVariable Long id, HttpServletRequest request) {
        SaleOrder order = saleOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!isOrderOwner(order, request)) {
            return Result.error("无权查看该订单");
        }
        return Result.success(order);
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id, HttpServletRequest request) {
        SaleOrder order = saleOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!isOrderOwner(order, request)) {
            return Result.error("无权查看该订单");
        }
        List<SaleOrderItem> items = saleOrderItemService.lambdaQuery()
                .eq(SaleOrderItem::getOrderId, id)
                .orderByAsc(SaleOrderItem::getSeqNo)
                .list();

        Customer customer = customerService.getById(order.getCustomerId());

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("customerName", customer != null ? customer.getCustomerName() : "");
        result.put("customerAddress", customer != null ? customer.getAddress() : "");
        result.put("customerContact", customer != null ? customer.getContactName() : "");
        result.put("customerPhone", customer != null ? customer.getPhone() : "");
        return Result.success(result);
    }

    @GetMapping
    public Result<List<SaleOrder>> listOrders(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<SaleOrder> list = saleOrderService.lambdaQuery()
                .eq(SaleOrder::getUserId, userId)
                .orderByDesc(SaleOrder::getCreateTime)
                .list();
        return Result.success(list);
    }

    /**
     * 分页条件查询订单列表（MyBatis-Plus 标准分页）
     * 支持多选查询：customerId、paymentMethod、status 可传入逗号分隔的多个值
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> pageOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderDateStart,
            @RequestParam(required = false) String orderDateEnd,
            HttpServletRequest request) {

        Long userId = getCurrentUserId(request);
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();

        // 用户隔离：只能查询自己创建的订单
        wrapper.eq(SaleOrder::getUserId, userId);

        // 客户多选：传入逗号分隔的客户ID列表
        if (customerId != null && !customerId.isEmpty()) {
            List<Long> customerIdList = Arrays.stream(customerId.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            if (!customerIdList.isEmpty()) {
                wrapper.in(SaleOrder::getCustomerId, customerIdList);
            }
        }

        // 收款方式多选：传入逗号分隔的方式列表
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            List<String> paymentMethodList = Arrays.stream(paymentMethod.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!paymentMethodList.isEmpty()) {
                wrapper.in(SaleOrder::getPaymentMethod, paymentMethodList);
            }
        }

        // 状态多选：传入逗号分隔的状态列表
        if (status != null && !status.isEmpty()) {
            List<String> statusList = Arrays.stream(status.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!statusList.isEmpty()) {
                wrapper.in(SaleOrder::getStatus, statusList);
            }
        }

        if (orderDateStart != null && !orderDateStart.isEmpty()) {
            wrapper.ge(SaleOrder::getOrderDate, LocalDate.parse(orderDateStart));
        }
        if (orderDateEnd != null && !orderDateEnd.isEmpty()) {
            wrapper.le(SaleOrder::getOrderDate, LocalDate.parse(orderDateEnd));
        }

        wrapper.orderByDesc(SaleOrder::getCreateTime);

        IPage<SaleOrder> resultPage = saleOrderService.page(page, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        return Result.success(result);
    }

    @PutMapping("/cancel/{id}")
    public Result<Boolean> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        SaleOrder order = saleOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!isOrderOwner(order, request)) {
            return Result.error("无权操作该订单");
        }
        if ("已作废".equals(order.getStatus())) {
            return Result.error("该订单已作废");
        }

        order.setStatus("已作废");
        order.setUpdateTime(LocalDateTime.now());
        saleOrderService.updateById(order);

        // 只要有欠款就回退：将该订单最新欠款记录的 remaining_amount 设为 0（标记无效），并减少用户-客户欠款
        if (order.getCurrentDebt() != null
                && order.getCurrentDebt().compareTo(BigDecimal.ZERO) > 0) {
            Long userId = getCurrentUserId(request);

            DebtRecord latestRecord = getLatestDebtRecord(order.getId());
            if (latestRecord != null) {
                latestRecord.setRemainingAmount(BigDecimal.ZERO);
                debtRecordService.updateById(latestRecord);
            }

            BigDecimal currentDebt = getUserCustomerDebt(userId, order.getCustomerId());
            updateUserCustomerDebt(userId, order.getCustomerId(), currentDebt.subtract(order.getCurrentDebt()));
        }

        return Result.success(true);
    }

    @PostMapping("/item")
    public Result<Boolean> saveItem(@RequestBody SaleOrderItem item) {
        return Result.success(saleOrderItemService.save(item));
    }

    @PostMapping("/item/batch")
    public Result<Boolean> saveItemsBatch(@RequestBody List<SaleOrderItem> items) {
        return Result.success(saleOrderItemService.saveBatch(items));
    }

    @DeleteMapping("/item/{id}")
    public Result<Boolean> deleteItem(@PathVariable Long id) {
        return Result.success(saleOrderItemService.removeById(id));
    }

    @GetMapping("/item/{orderId}")
    public Result<List<SaleOrderItem>> listItemsByOrderId(@PathVariable Long orderId) {
        return Result.success(saleOrderItemService.lambdaQuery()
                .eq(SaleOrderItem::getOrderId, orderId)
                .orderByAsc(SaleOrderItem::getSeqNo)
                .list());
    }

    @PostMapping("/debt")
    public Result<Boolean> saveDebt(@RequestBody DebtRecord debtRecord, HttpServletRequest request) {
        debtRecord.setUserId(getCurrentUserId(request));
        return Result.success(debtRecordService.save(debtRecord));
    }

    /**
     * 查询当前用户对指定客户的欠款记录
     */
    @GetMapping("/debt/{customerId}")
    public Result<List<DebtRecord>> listDebtsByCustomerId(@PathVariable Long customerId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return Result.success(debtRecordService.lambdaQuery()
                .eq(DebtRecord::getUserId, userId)
                .eq(DebtRecord::getCustomerId, customerId)
                .orderByDesc(DebtRecord::getCreateTime)
                .list());
    }

    /**
     * 查询当前用户的所有欠款记录（带客户名称）
     * 用于欠款列表页面展示
     * 支持筛选：客户多选、创建时间范围
     */
    @GetMapping("/debts/all")
    public Result<List<Map<String, Object>>> listAllDebts(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            HttpServletRequest request) {

        Long userId = getCurrentUserId(request);

        // 构建查询条件
        LambdaQueryWrapper<DebtRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DebtRecord::getUserId, userId);

        // 客户多选筛选
        if (customerId != null && !customerId.isEmpty()) {
            List<Long> customerIdList = Arrays.stream(customerId.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
            if (!customerIdList.isEmpty()) {
                wrapper.in(DebtRecord::getCustomerId, customerIdList);
            }
        }

        // 创建时间范围筛选
        if (dateStart != null && !dateStart.isEmpty()) {
            wrapper.ge(DebtRecord::getCreateTime, LocalDate.parse(dateStart).atStartOfDay());
        }
        if (dateEnd != null && !dateEnd.isEmpty()) {
            wrapper.le(DebtRecord::getCreateTime, LocalDate.parse(dateEnd).atTime(23, 59, 59));
        }

        wrapper.orderByDesc(DebtRecord::getCreateTime);

        List<DebtRecord> debts = debtRecordService.list(wrapper);

        // 查询客户信息用于关联
        List<Long> customerIds = debts.stream()
                .map(DebtRecord::getCustomerId)
                .distinct()
                .toList();

        Map<Long, String> customerNameMap = new HashMap<>();
        if (!customerIds.isEmpty()) {
            customerNameMap.putAll(customerService.lambdaQuery()
                    .in(Customer::getId, customerIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Customer::getId, Customer::getCustomerName, (a, b) -> a)));
        }

        // 组装返回数据
        List<Map<String, Object>> result = debts.stream().map(debt -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", debt.getId());
            map.put("customerId", debt.getCustomerId());
            map.put("customerName", customerNameMap.getOrDefault(debt.getCustomerId(), ""));
            map.put("orderId", debt.getOrderId());
            map.put("orderNo", debt.getOrderNo());
            map.put("amount", debt.getAmount());
            map.put("remainingAmount", debt.getRemainingAmount());
            map.put("type", debt.getType());
            map.put("remark", debt.getRemark());
            map.put("createTime", debt.getCreateTime());
            return map;
        }).toList();

        return Result.success(result);
    }
}
