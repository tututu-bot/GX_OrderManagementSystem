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
     * 客户还款（核心接口）
     *
     * 流程：
     * 1. 创建 repayment_record 记录本次还款
     * 2. 按时间先后顺序遍历该客户的未结清欠款，逐笔核销
     * 3. 每笔核销创建 debt_repayment 关联记录
     * 4. 更新对应 debt_record 的 settled_amount 和 status
     * 5. 从 debt_record 实时计算并同步 user_customer.debt_amount
     */
    @PostMapping
    public Result<Boolean> saveRepayment(@RequestBody RepaymentRecord repayment, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        if (repayment.getCustomerId() == null) {
            return Result.error("请选择客户");
        }
        if (repayment.getAmount() == null || repayment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("还款金额必须大于 0");
        }

        // 计算该客户当前未结清欠款总额
        BigDecimal totalUnsettled = calcUnsettledDebt(userId, repayment.getCustomerId());
        if (repayment.getAmount().compareTo(totalUnsettled) > 0) {
            return Result.error("还款金额不能大于未结清欠款总额（当前欠款：" + totalUnsettled + " 元）");
        }

        // 创建还款记录
        repayment.setUserId(userId);
        if (repayment.getRepaymentDate() == null) {
            repayment.setRepaymentDate(LocalDate.now());
        }
        repaymentRecordService.save(repayment);

        // 按时间顺序核销未结清欠款
        settleDebts(userId, repayment.getCustomerId(), repayment.getAmount(), repayment.getId());

        // 同步用户-客户欠款
        syncUserCustomerDebt(userId, repayment.getCustomerId());

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
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        Page<RepaymentRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepaymentRecord::getUserId, userId);
        if (customerId != null) {
            wrapper.eq(RepaymentRecord::getCustomerId, customerId);
        }
        wrapper.orderByDesc(RepaymentRecord::getCreateTime);
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
     * 计算某客户未结清欠款总额
     */
    private BigDecimal calcUnsettledDebt(Long userId, Long customerId) {
        List<DebtRecord> debts = debtRecordService.lambdaQuery()
                .eq(DebtRecord::getUserId, userId)
                .eq(DebtRecord::getCustomerId, customerId)
                .in(DebtRecord::getStatus, Arrays.asList("UNSETTLED", "PARTIAL"))
                .list();

        return debts.stream()
                .map(d -> d.getAmount().subtract(
                        d.getSettledAmount() != null ? d.getSettledAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 按时间顺序核销该客户的未结清欠款
     *
     * @param remainingAmount 待核销金额
     * @param repaymentId     关联的还款记录ID
     */
    private void settleDebts(Long userId, Long customerId, BigDecimal remainingAmount, Long repaymentId) {
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

        // 记录被完全结清的订单ID
        Set<Long> completedOrderIds = new HashSet<>();
        BigDecimal remaining = remainingAmount;
        for (DebtRecord debt : unsettledDebts) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal debtRemaining = debt.getAmount().subtract(
                    debt.getSettledAmount() != null ? debt.getSettledAmount() : BigDecimal.ZERO);

            if (debtRemaining.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal settleAmount = remaining.min(debtRemaining);

            // 创建核销关联
            DebtRepayment dr = new DebtRepayment();
            dr.setDebtId(debt.getId());
            dr.setRepaymentId(repaymentId);
            dr.setAmount(settleAmount);
            debtRepaymentService.save(dr);

            // 更新欠款记录
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

        // 检查每个被完全结清的订单，是否所有欠款都已结清，是则改为"已完成"
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
