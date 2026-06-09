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
     * 从 debt_record 实时计算并同步 user_customer 的欠款金额
     * 说明: 每次欠款变动后调用，确保 user_customer.debt_amount 与实际欠款一致
     */
    private void syncUserCustomerDebt(Long userId, Long customerId) {
        List<DebtRecord> debts = debtRecordService.lambdaQuery()
                .eq(DebtRecord::getUserId, userId)
                .eq(DebtRecord::getCustomerId, customerId)
                .in(DebtRecord::getStatus, Arrays.asList("UNSETTLED", "PARTIAL"))
                .list();

        BigDecimal totalUnsettled = debts.stream()
                .map(d -> d.getAmount().subtract(
                        d.getSettledAmount() != null ? d.getSettledAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UserCustomer uc = userCustomerService.lambdaQuery()
                .eq(UserCustomer::getUserId, userId)
                .eq(UserCustomer::getCustomerId, customerId)
                .one();

        if (uc == null) {
            uc = new UserCustomer();
            uc.setUserId(userId);
            uc.setCustomerId(customerId);
            uc.setDebtAmount(totalUnsettled);
            userCustomerService.save(uc);
        } else {
            uc.setDebtAmount(totalUnsettled);
            userCustomerService.updateById(uc);
        }
    }

    @PostMapping
    public Result<Map<String, Object>> saveOrder(@RequestBody SaleOrder order, HttpServletRequest request) {
        if (!isCustomerLinked(order.getCustomerId(), request)) {
            return Result.error("无权为该客户创建订单");
        }

        Long userId = getCurrentUserId(request);
        order.setUserId(userId);

        BigDecimal receivableAmount = order.getReceivableAmount() != null ? order.getReceivableAmount() : BigDecimal.ZERO;
        BigDecimal receivedAmount = order.getReceivedAmount() != null ? order.getReceivedAmount() : BigDecimal.ZERO;
        BigDecimal currentDebt = order.getCurrentDebt() != null ? order.getCurrentDebt() : BigDecimal.ZERO;

        // 验证：receivedAmount + currentDebt = receivableAmount
        if (receivedAmount.add(currentDebt).compareTo(receivableAmount) != 0) {
            return Result.error("本次收款 + 本次欠款 必须等于 应收金额");
        }

        if (currentDebt.compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus("已完成");
        }

        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        boolean saved = saleOrderService.save(order);
        if (!saved) {
            return Result.error("保存订单失败");
        }

        // 一个订单只对应一条欠款记录：amount=应收金额，settledAmount=已收款
        if (currentDebt.compareTo(BigDecimal.ZERO) > 0) {
            long existingCount = debtRecordService.lambdaQuery()
                    .eq(DebtRecord::getOrderId, order.getId())
                    .count();
            if (existingCount == 0) {
                DebtRecord debt = new DebtRecord();
                debt.setUserId(userId);
                debt.setCustomerId(order.getCustomerId());
                debt.setOrderId(order.getId());
                debt.setOrderNo(order.getOrderNo());
                debt.setAmount(receivableAmount);
                debt.setSettledAmount(receivedAmount);
                debt.setStatus(receivedAmount.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "UNSETTLED");
                debt.setRemark("订单欠款，收款方式：" + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "未知"));
                debt.setCreateTime(LocalDateTime.now());
                debtRecordService.save(debt);
            }
            syncUserCustomerDebt(userId, order.getCustomerId());
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
     * 修改订单（一个订单最多一条欠款记录，直接同步）
     */
    @PutMapping
    public Result<Boolean> updateOrder(@RequestBody SaleOrder order, HttpServletRequest request) {
        SaleOrder oldOrder = saleOrderService.getById(order.getId());
        if (oldOrder == null) {
            return Result.error("订单不存在");
        }
        if (!isOrderOwner(oldOrder, request)) {
            return Result.error("无权修改该订单");
        }
        if ("已作废".equals(oldOrder.getStatus())) {
            return Result.error("已作废的订单不能修改");
        }

        Long userId = getCurrentUserId(request);

        BigDecimal receivableAmount = order.getReceivableAmount() != null ? order.getReceivableAmount() : BigDecimal.ZERO;
        BigDecimal receivedAmount = order.getReceivedAmount() != null ? order.getReceivedAmount() : BigDecimal.ZERO;
        BigDecimal currentDebt = order.getCurrentDebt() != null ? order.getCurrentDebt() : BigDecimal.ZERO;

        // 验证金额关系
        if (receivedAmount.add(currentDebt).compareTo(receivableAmount) != 0) {
            return Result.error("本次收款 + 本次欠款 必须等于 应收金额");
        }

        // 不允许修改本次收款
        BigDecimal oldReceivedAmount = oldOrder.getReceivedAmount() != null ? oldOrder.getReceivedAmount() : BigDecimal.ZERO;
        if (receivedAmount.compareTo(oldReceivedAmount) != 0) {
            return Result.error("修改订单时不允许修改本次收款");
        }

        // order_no 是 UNIQUE，不能更新
        order.setOrderNo(null);
        order.setUpdateTime(LocalDateTime.now());
        boolean updated = saleOrderService.updateById(order);
        if (!updated) {
            return Result.error("更新订单失败");
        }

        // 同步欠款记录：一个订单最多一条
        DebtRecord existingDebt = debtRecordService.lambdaQuery()
                .eq(DebtRecord::getOrderId, order.getId())
                .one();

        if (currentDebt.compareTo(BigDecimal.ZERO) > 0) {
            if (existingDebt != null) {
                existingDebt.setAmount(receivableAmount);
                existingDebt.setSettledAmount(receivedAmount);
                existingDebt.setStatus(receivedAmount.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "UNSETTLED");
                debtRecordService.updateById(existingDebt);
            } else {
                DebtRecord debt = new DebtRecord();
                debt.setUserId(userId);
                debt.setCustomerId(order.getCustomerId());
                debt.setOrderId(order.getId());
                debt.setOrderNo(oldOrder.getOrderNo());
                debt.setAmount(receivableAmount);
                debt.setSettledAmount(receivedAmount);
                debt.setStatus(receivedAmount.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "UNSETTLED");
                debt.setRemark("订单修改，新增欠款");
                debt.setCreateTime(LocalDateTime.now());
                debtRecordService.save(debt);
            }
            syncUserCustomerDebt(userId, order.getCustomerId());
        } else {
            if (existingDebt != null && !"SETTLED".equals(existingDebt.getStatus())) {
                existingDebt.setSettledAmount(existingDebt.getAmount());
                existingDebt.setStatus("SETTLED");
                debtRecordService.updateById(existingDebt);
                syncUserCustomerDebt(userId, order.getCustomerId());
            }
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
        result.put("customerCoreProduct", customer != null ? customer.getCoreProduct() : "");
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
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderDateStart,
            @RequestParam(required = false) String orderDateEnd,
            @RequestParam(defaultValue = "createTime") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            HttpServletRequest request) {

        Long userId = getCurrentUserId(request);
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();

        // 用户隔离：只能查询自己创建的订单
        wrapper.eq(SaleOrder::getUserId, userId);

        // 客户名称模糊搜索：先查匹配的客户ID，再过滤订单
        if (customerName != null && !customerName.isBlank()) {
            List<Long> matchedCustomerIds = customerService.lambdaQuery()
                    .like(Customer::getCustomerName, customerName)
                    .list()
                    .stream()
                    .map(Customer::getId)
                    .collect(Collectors.toList());
            if (!matchedCustomerIds.isEmpty()) {
                wrapper.in(SaleOrder::getCustomerId, matchedCustomerIds);
            } else {
                // 没有匹配的客户，返回空结果
                Map<String, Object> empty = new HashMap<>();
                empty.put("list", List.of());
                empty.put("total", 0L);
                empty.put("pages", 0L);
                return Result.success(empty);
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

        // 动态排序（白名单映射，默认 createTime 降序）
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("orderDate".equals(sortField)) {
            if (asc) wrapper.orderByAsc(SaleOrder::getOrderDate);
            else wrapper.orderByDesc(SaleOrder::getOrderDate);
        } else {
            if (asc) wrapper.orderByAsc(SaleOrder::getCreateTime);
            else wrapper.orderByDesc(SaleOrder::getCreateTime);
        }

        IPage<SaleOrder> resultPage = saleOrderService.page(page, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        return Result.success(result);
    }

    /**
     * 作废订单：找到对应欠款记录并结清，备注标明结清方式为作废订单
     */
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

        Long userId = getCurrentUserId(request);

        // 找到该订单的欠款记录（一对一），直接结清
        DebtRecord debt = debtRecordService.lambdaQuery()
                .eq(DebtRecord::getOrderId, id)
                .one();
        if (debt != null && !"SETTLED".equals(debt.getStatus())) {
            debt.setSettledAmount(debt.getAmount());
            debt.setStatus("SETTLED");
            String oldRemark = debt.getRemark() != null ? debt.getRemark() : "";
            debt.setRemark(oldRemark + "（结清方式：作废订单）");
            debtRecordService.updateById(debt);
            syncUserCustomerDebt(userId, order.getCustomerId());
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
     * 分页条件查询当前用户的所有欠款记录（带客户名称）
     * 用于欠款列表页面展示
     * 支持筛选：客户多选、单据编号、创建时间范围
     */
    @GetMapping("/debts/all")
    public Result<Map<String, Object>> listAllDebts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(defaultValue = "createTime") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
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

        // 单据编号模糊查询
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(DebtRecord::getOrderNo, orderNo);
        }

        // 创建时间范围筛选
        if (dateStart != null && !dateStart.isEmpty()) {
            wrapper.ge(DebtRecord::getCreateTime, LocalDate.parse(dateStart).atStartOfDay());
        }
        if (dateEnd != null && !dateEnd.isEmpty()) {
            wrapper.le(DebtRecord::getCreateTime, LocalDate.parse(dateEnd).atTime(23, 59, 59));
        }

        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(DebtRecord::getCreateTime);
        } else {
            wrapper.orderByDesc(DebtRecord::getCreateTime);
        }

        Page<DebtRecord> page = new Page<>(pageNum, pageSize);
        debtRecordService.page(page, wrapper);

        // 查询客户信息用于关联
        List<Long> customerIds = page.getRecords().stream()
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
        List<Map<String, Object>> list = page.getRecords().stream().map(debt -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", debt.getId());
            map.put("customerId", debt.getCustomerId());
            map.put("customerName", customerNameMap.getOrDefault(debt.getCustomerId(), ""));
            map.put("orderId", debt.getOrderId());
            map.put("orderNo", debt.getOrderNo());
            BigDecimal settled = debt.getSettledAmount() != null ? debt.getSettledAmount() : BigDecimal.ZERO;
            map.put("amount", debt.getAmount());
            map.put("settledAmount", settled);
            map.put("unsettledAmount", debt.getAmount().subtract(settled));
            map.put("status", debt.getStatus());
            map.put("remark", debt.getRemark());
            map.put("createTime", debt.getCreateTime());
            return map;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return Result.success(result);
    }
}
