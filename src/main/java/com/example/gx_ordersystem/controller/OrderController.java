package com.example.gx_ordersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Customer;
import com.example.gx_ordersystem.entity.DebtRecord;
import com.example.gx_ordersystem.entity.DebtRepayment;
import com.example.gx_ordersystem.entity.RepaymentRecord;
import com.example.gx_ordersystem.entity.SaleOrder;
import com.example.gx_ordersystem.entity.SaleOrderItem;
import com.example.gx_ordersystem.entity.UserCustomer;
import com.example.gx_ordersystem.service.CustomerService;
import com.example.gx_ordersystem.service.DebtRecordService;
import com.example.gx_ordersystem.service.DebtRepaymentService;
import com.example.gx_ordersystem.service.RepaymentRecordService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    @Autowired
    private RepaymentRecordService repaymentRecordService;
    @Autowired
    private DebtRepaymentService debtRepaymentService;

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
     * 创建欠款记录
     *
     * @param amount         原始欠款金额
     * @param settledAmount  已还金额（新建时通常为 0）
     * @param status         状态: UNSETTLED / PARTIAL / SETTLED
     */
    private void createDebtRecord(Long userId, Long customerId, Long orderId, String orderNo,
                                  BigDecimal amount, BigDecimal settledAmount, String status, String remark) {
        DebtRecord debtRecord = new DebtRecord();
        debtRecord.setUserId(userId);
        debtRecord.setCustomerId(customerId);
        debtRecord.setOrderId(orderId);
        debtRecord.setOrderNo(orderNo);
        debtRecord.setAmount(amount);
        debtRecord.setSettledAmount(settledAmount != null ? settledAmount : BigDecimal.ZERO);
        debtRecord.setStatus(status);
        debtRecord.setRemark(remark);
        debtRecordService.save(debtRecord);
    }

    /**
     * 创建还款记录并按时间顺序核销该客户的未结清欠款
     *
     * @param amount        还款金额
     * @param remark        还款备注
     * @param paymentMethod 还款方式（可为 null）
     */
    private void createRepaymentAndSettle(Long userId, Long customerId, BigDecimal amount,
                                          String remark, String paymentMethod) {
        // 1. 创建还款记录
        RepaymentRecord repayment = new RepaymentRecord();
        repayment.setUserId(userId);
        repayment.setCustomerId(customerId);
        repayment.setAmount(amount);
        repayment.setPaymentMethod(paymentMethod);
        repayment.setRepaymentDate(LocalDate.now());
        repayment.setRemark(remark);
        repaymentRecordService.save(repayment);

        // 2. 查询该客户所有未结清的欠款记录（按时间先后，先欠的先还）
        List<DebtRecord> unsettledDebts = debtRecordService.lambdaQuery()
                .eq(DebtRecord::getUserId, userId)
                .eq(DebtRecord::getCustomerId, customerId)
                .in(DebtRecord::getStatus, Arrays.asList("UNSETTLED", "PARTIAL"))
                .orderByAsc(DebtRecord::getCreateTime)
                .orderByAsc(DebtRecord::getId)
                .list();

        System.out.println("[核销顺序] 共" + unsettledDebts.size() + "笔未结清欠款，核销顺序：");
        for (int i = 0; i < unsettledDebts.size(); i++) {
            DebtRecord d = unsettledDebts.get(i);
            System.out.println("  " + (i + 1) + ". id=" + d.getId() + " orderNo=" + d.getOrderNo()
                    + " amount=" + d.getAmount() + " settled=" + d.getSettledAmount()
                    + " createTime=" + d.getCreateTime());
        }

        // 3. 逐笔核销，记录被完全结清的订单ID
        Set<Long> completedOrderIds = new HashSet<>();
        BigDecimal remaining = amount;
        for (DebtRecord debt : unsettledDebts) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal debtRemaining = debt.getAmount().subtract(
                    debt.getSettledAmount() != null ? debt.getSettledAmount() : BigDecimal.ZERO);

            if (debtRemaining.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal settleAmount = remaining.min(debtRemaining);

            // 创建核销关联
            DebtRepayment dr = new DebtRepayment();
            dr.setDebtId(debt.getId());
            dr.setRepaymentId(repayment.getId());
            dr.setAmount(settleAmount);
            debtRepaymentService.save(dr);

            // 更新欠款记录的已还金额和状态
            BigDecimal newSettled = (debt.getSettledAmount() != null ? debt.getSettledAmount() : BigDecimal.ZERO)
                    .add(settleAmount);
            debt.setSettledAmount(newSettled);
            if (newSettled.compareTo(debt.getAmount()) >= 0) {
                debt.setStatus("SETTLED");
                // 记录该欠款对应的订单ID，后续检查是否全部结清
                if (debt.getOrderId() != null) {
                    completedOrderIds.add(debt.getOrderId());
                }
            } else {
                debt.setStatus("PARTIAL");
            }
            debtRecordService.updateById(debt);

            remaining = remaining.subtract(settleAmount);
        }

        // 4. 检查每个被完全结清的订单，是否所有欠款都已结清，是则改为"已完成"
        for (Long orderId : completedOrderIds) {
            checkAndCompleteOrder(orderId);
        }
    }

    /**
     * 检查指定订单的所有欠款是否已全部结清，如果是则更新订单状态为"已完成"
     */
    private void checkAndCompleteOrder(Long orderId) {
        if (orderId == null) return;

        // 直接查询该订单是否还有未结清/部分结清的欠款记录
        long unsettledCount = debtRecordService.lambdaQuery()
                .eq(DebtRecord::getOrderId, orderId)
                .in(DebtRecord::getStatus, Arrays.asList("UNSETTLED", "PARTIAL"))
                .count();

        System.out.println("[checkAndCompleteOrder] orderId=" + orderId + ", 未结清欠款笔数=" + unsettledCount);

        if (unsettledCount == 0) {
            SaleOrder order = saleOrderService.getById(orderId);
            System.out.println("[checkAndCompleteOrder] 订单状态=" + (order != null ? order.getStatus() : "null"));
            if (order != null && "进行中".equals(order.getStatus())) {
                order.setStatus("已完成");
                order.setUpdateTime(LocalDateTime.now());
                boolean updated = saleOrderService.updateById(order);
                System.out.println("[checkAndCompleteOrder] 订单状态更新为已完成, result=" + updated);
            }
        }
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
        // 验证客户是否被当前用户关联
        if (!isCustomerLinked(order.getCustomerId(), request)) {
            return Result.error("无权为该客户创建订单");
        }

        // 设置订单创建者
        order.setUserId(getCurrentUserId(request));

        // 本次欠款为0（全额付款），状态直接为"已完成"
        BigDecimal currentDebt = order.getCurrentDebt() != null ? order.getCurrentDebt() : BigDecimal.ZERO;
        if (currentDebt.compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus("已完成");
        }

        boolean saved = saleOrderService.save(order);
        if (!saved) {
            return Result.error("保存订单失败");
        }

        Long userId = getCurrentUserId(request);

        // 有欠款时创建欠款记录，并从 debt_record 实时同步用户-客户欠款
        if (currentDebt.compareTo(BigDecimal.ZERO) > 0) {
            createDebtRecord(userId, order.getCustomerId(), order.getId(), order.getOrderNo(),
                    currentDebt, BigDecimal.ZERO, "UNSETTLED",
                    "订单欠款，收款方式：" + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "未知"));

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
     * 修改订单（同时处理欠款金额变化）
     *
     * 欠款变动处理逻辑：
     * - 新欠款 > 旧欠款：差额视为新增欠款，创建 debt_record
     * - 新欠款 < 旧欠款：差额视为还款，创建 repayment_record 并按顺序核销
     * - 新欠款 = 旧欠款：无变化
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

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            // 欠款增加：新增欠款记录
            createDebtRecord(userId, order.getCustomerId(), order.getId(), oldOrder.getOrderNo(),
                    diff, BigDecimal.ZERO, "UNSETTLED",
                    "订单修改，新增欠款，收款方式：" + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "未知"));
            syncUserCustomerDebt(userId, order.getCustomerId());
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            // 欠款减少：视为还款，创建还款记录并核销
            createRepaymentAndSettle(userId, order.getCustomerId(), diff.abs(),
                    "订单修改，欠款减少", order.getPaymentMethod());
            syncUserCustomerDebt(userId, order.getCustomerId());
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

    /**
     * 作废订单
     *
     * 欠款回退逻辑：
     * - 若订单有欠款，创建还款记录并全额核销该订单关联的未结清欠款
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
        BigDecimal currentDebt = order.getCurrentDebt() != null ? order.getCurrentDebt() : BigDecimal.ZERO;

        // 有欠款时，创建还款记录并全额核销（先欠先还，可能核销到多张订单的欠款）
        if (currentDebt.compareTo(BigDecimal.ZERO) > 0) {
            createRepaymentAndSettle(userId, order.getCustomerId(), currentDebt,
                    "订单作废退款", null);
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

        wrapper.orderByDesc(DebtRecord::getCreateTime);

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
