package com.example.gx_ordersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Customer;
import com.example.gx_ordersystem.entity.DebtRecord;
import com.example.gx_ordersystem.entity.DebtRepayment;
import com.example.gx_ordersystem.entity.RepaymentRecord;
import com.example.gx_ordersystem.entity.SaleOrder;
import com.example.gx_ordersystem.entity.UserCustomer;
import com.example.gx_ordersystem.service.CustomerService;
import com.example.gx_ordersystem.service.DebtRecordService;
import com.example.gx_ordersystem.service.DebtRepaymentService;
import com.example.gx_ordersystem.service.RepaymentRecordService;
import com.example.gx_ordersystem.service.SaleOrderService;
import com.example.gx_ordersystem.service.UserCustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 还款管理控制器
 * 处理客户还款相关的 HTTP 请求，提供还款登记、还款记录查询、欠款查询等 RESTful API
 *
 * 核心设计：
 * - 还款是独立的业务事件，与订单解耦
 * - 一笔还款可以核销多笔欠款（按时间先后顺序，先欠的先还）
 * - 通过 debt_repayment 关联表精确记录每笔还款核销了哪些欠款的多少金额
 */
@RestController
@RequestMapping("/api/repayment")
public class RepaymentController {

    @Autowired
    private RepaymentRecordService repaymentRecordService;
    @Autowired
    private DebtRecordService debtRecordService;
    @Autowired
    private DebtRepaymentService debtRepaymentService;
    @Autowired
    private UserCustomerService userCustomerService;
    @Autowired
    private SaleOrderService saleOrderService;
    @Autowired
    private CustomerService customerService;

    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * 逐笔还款：针对指定欠款记录进行还款
     * 还完后同步更新对应订单的 receivedAmount 和 currentDebt
     */
    @PostMapping
    public Result<Boolean> saveRepayment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        Long debtId = Long.valueOf(body.get("debtId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String paymentMethod = body.get("paymentMethod") != null ? body.get("paymentMethod").toString() : null;
        String remark = body.get("remark") != null ? body.get("remark").toString() : null;
        String repaymentDateStr = body.get("repaymentDate") != null ? body.get("repaymentDate").toString() : null;

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("还款金额必须大于 0");
        }

        DebtRecord debt = debtRecordService.getById(debtId);
        if (debt == null) {
            return Result.error("欠款记录不存在");
        }
        if (!userId.equals(debt.getUserId())) {
            return Result.error("无权操作该欠款记录");
        }

        BigDecimal unsettled = debt.getAmount().subtract(
                debt.getSettledAmount() != null ? debt.getSettledAmount() : BigDecimal.ZERO);
        if (amount.compareTo(unsettled) > 0) {
            return Result.error("还款金额不能大于剩余欠款（剩余：" + unsettled + " 元）");
        }

        // 1. 创建还款记录
        RepaymentRecord repayment = new RepaymentRecord();
        repayment.setUserId(userId);
        repayment.setCustomerId(debt.getCustomerId());
        repayment.setAmount(amount);
        repayment.setPaymentMethod(paymentMethod);
        repayment.setRemark(remark);
        if (repaymentDateStr != null && !repaymentDateStr.isBlank()) {
            repayment.setRepaymentDate(LocalDate.parse(repaymentDateStr));
        } else {
            repayment.setRepaymentDate(LocalDate.now());
        }
        repayment.setCreateTime(LocalDateTime.now());
        repaymentRecordService.save(repayment);

        // 2. 创建核销关联
        DebtRepayment dr = new DebtRepayment();
        dr.setDebtId(debtId);
        dr.setRepaymentId(repayment.getId());
        dr.setAmount(amount);
        debtRepaymentService.save(dr);

        // 3. 更新欠款记录
        BigDecimal oldSettled = debt.getSettledAmount() != null ? debt.getSettledAmount() : BigDecimal.ZERO;
        BigDecimal newSettled = oldSettled.add(amount);
        debt.setSettledAmount(newSettled);
        debt.setStatus(newSettled.compareTo(debt.getAmount()) >= 0 ? "SETTLED" : "PARTIAL");
        debtRecordService.updateById(debt);

        // 4. 同步订单：receivedAmount += amount，currentDebt -= amount
        if (debt.getOrderId() != null) {
            SaleOrder order = saleOrderService.getById(debt.getOrderId());
            if (order != null) {
                BigDecimal orderReceived = order.getReceivedAmount() != null ? order.getReceivedAmount() : BigDecimal.ZERO;
                BigDecimal orderReceivable = order.getReceivableAmount() != null ? order.getReceivableAmount() : BigDecimal.ZERO;
                order.setReceivedAmount(orderReceived.add(amount));
                order.setCurrentDebt(orderReceivable.subtract(order.getReceivedAmount()));
                if ("SETTLED".equals(debt.getStatus()) && "进行中".equals(order.getStatus())) {
                    order.setStatus("已完成");
                }
                order.setUpdateTime(LocalDateTime.now());
                saleOrderService.updateById(order);
            }
        }

        // 5. 同步用户-客户欠款
        syncUserCustomerDebt(userId, debt.getCustomerId());

        return Result.success(true);
    }

    /**
     * 分页查询当前用户的还款记录列表（带客户名称）
     */
    @GetMapping
    public Result<Map<String, Object>> listRepayments(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "createTime") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        Page<RepaymentRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepaymentRecord::getUserId, userId);
        if (customerId != null) {
            wrapper.eq(RepaymentRecord::getCustomerId, customerId);
        }
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("repaymentDate".equals(sortField)) {
            if (asc) wrapper.orderByAsc(RepaymentRecord::getRepaymentDate);
            else wrapper.orderByDesc(RepaymentRecord::getRepaymentDate);
        } else {
            if (asc) wrapper.orderByAsc(RepaymentRecord::getCreateTime);
            else wrapper.orderByDesc(RepaymentRecord::getCreateTime);
        }
        repaymentRecordService.page(page, wrapper);

        // 查询客户名称
        List<Long> customerIds = page.getRecords().stream()
                .map(RepaymentRecord::getCustomerId)
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

        List<Map<String, Object>> list = page.getRecords().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("customerId", r.getCustomerId());
            map.put("customerName", customerNameMap.getOrDefault(r.getCustomerId(), ""));
            map.put("amount", r.getAmount());
            map.put("paymentMethod", r.getPaymentMethod());
            map.put("repaymentDate", r.getRepaymentDate());
            map.put("remark", r.getRemark());
            map.put("createTime", r.getCreateTime());
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

    /**
     * 查询某客户的未结清欠款明细
     * 返回每笔欠款的原始金额、已还金额、剩余未还金额
     */
    @GetMapping("/debts/{customerId}")
    public Result<List<Map<String, Object>>> listUnsettledDebts(
            @PathVariable Long customerId, HttpServletRequest request) {

        Long userId = getCurrentUserId(request);
        List<DebtRecord> debts = debtRecordService.lambdaQuery()
                .eq(DebtRecord::getUserId, userId)
                .eq(DebtRecord::getCustomerId, customerId)
                .in(DebtRecord::getStatus, Arrays.asList("UNSETTLED", "PARTIAL"))
                .orderByAsc(DebtRecord::getCreateTime)
                .list();

        List<Map<String, Object>> result = debts.stream().map(debt -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", debt.getId());
            map.put("orderId", debt.getOrderId());
            map.put("orderNo", debt.getOrderNo());
            map.put("amount", debt.getAmount());
            map.put("settledAmount", debt.getSettledAmount() != null ? debt.getSettledAmount() : BigDecimal.ZERO);
            map.put("unsettledAmount", debt.getAmount().subtract(
                    debt.getSettledAmount() != null ? debt.getSettledAmount() : BigDecimal.ZERO));
            map.put("status", debt.getStatus());
            map.put("remark", debt.getRemark());
            map.put("createTime", debt.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 查询客户欠款汇总
     * 返回：未结清欠款总额、欠款笔数、最近一次欠款时间
     */
    @GetMapping("/summary/{customerId}")
    public Result<Map<String, Object>> getDebtSummary(@PathVariable Long customerId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        List<DebtRecord> debts = debtRecordService.lambdaQuery()
                .eq(DebtRecord::getUserId, userId)
                .eq(DebtRecord::getCustomerId, customerId)
                .in(DebtRecord::getStatus, Arrays.asList("UNSETTLED", "PARTIAL"))
                .list();

        BigDecimal totalUnsettled = debts.stream()
                .map(d -> d.getAmount().subtract(
                        d.getSettledAmount() != null ? d.getSettledAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("customerId", customerId);
        result.put("totalUnsettled", totalUnsettled);
        result.put("debtCount", debts.size());
        result.put("latestDebtTime", debts.isEmpty() ? null :
                debts.stream().max(Comparator.comparing(DebtRecord::getCreateTime))
                        .map(DebtRecord::getCreateTime).orElse(null));

        return Result.success(result);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从 debt_record 实时计算并同步 user_customer 的欠款金额
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
}
