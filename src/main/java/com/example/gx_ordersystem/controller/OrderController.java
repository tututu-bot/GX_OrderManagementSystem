package com.example.gx_ordersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Customer;
import com.example.gx_ordersystem.entity.DebtRecord;
import com.example.gx_ordersystem.entity.SaleOrder;
import com.example.gx_ordersystem.entity.SaleOrderItem;
import com.example.gx_ordersystem.service.CustomerService;
import com.example.gx_ordersystem.service.DebtRecordService;
import com.example.gx_ordersystem.service.SaleOrderItemService;
import com.example.gx_ordersystem.service.SaleOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单管理控制器
 * 处理销售订单相关的HTTP请求，提供订单、订单明细、欠款记录的增删改查RESTful API
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

    @PostMapping
    public Result<Map<String, Object>> saveOrder(@RequestBody SaleOrder order) {
        boolean saved = saleOrderService.save(order);
        if (!saved) {
            return Result.error("保存订单失败");
        }

        if ("赊账".equals(order.getPaymentMethod())
                && order.getCurrentDebt() != null
                && order.getCurrentDebt().compareTo(BigDecimal.ZERO) > 0) {
            DebtRecord debtRecord = new DebtRecord();
            debtRecord.setCustomerId(order.getCustomerId());
            debtRecord.setOrderId(order.getId());
            debtRecord.setOrderNo(order.getOrderNo());
            debtRecord.setAmount(order.getCurrentDebt());
            debtRecord.setType("NEW");
            debtRecord.setRemark("订单欠款，收款方式：" + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "未知"));
            debtRecordService.save(debtRecord);

            Customer customer = customerService.getById(order.getCustomerId());
            if (customer != null) {
                BigDecimal newDebt = (customer.getTotalDebt() != null ? customer.getTotalDebt() : BigDecimal.ZERO)
                        .add(order.getCurrentDebt());
                customer.setTotalDebt(newDebt);
                customerService.updateById(customer);
            }
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

    @PutMapping
    public Result<Boolean> updateOrder(@RequestBody SaleOrder order) {
        return Result.success(saleOrderService.updateById(order));
    }

    @GetMapping("/{id}")
    public Result<SaleOrder> getOrderById(@PathVariable Long id) {
        return Result.success(saleOrderService.getById(id));
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        SaleOrder order = saleOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
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
        return Result.success(result);
    }

    @GetMapping
    public Result<List<SaleOrder>> listOrders() {
        return Result.success(saleOrderService.list());
    }

    /**
     * 分页条件查询订单列表（MyBatis-Plus 标准分页）
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> pageOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderDateStart,
            @RequestParam(required = false) String orderDateEnd) {

        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();

        if (customerId != null) {
            wrapper.eq(SaleOrder::getCustomerId, customerId);
        }
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            wrapper.eq(SaleOrder::getPaymentMethod, paymentMethod);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SaleOrder::getStatus, status);
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
    public Result<Boolean> cancelOrder(@PathVariable Long id) {
        SaleOrder order = saleOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if ("已作废".equals(order.getStatus())) {
            return Result.error("该订单已作废");
        }

        order.setStatus("已作废");
        order.setUpdateTime(LocalDateTime.now());
        saleOrderService.updateById(order);

        if ("赊账".equals(order.getPaymentMethod())
                && order.getCurrentDebt() != null
                && order.getCurrentDebt().compareTo(BigDecimal.ZERO) > 0) {
            DebtRecord repayRecord = new DebtRecord();
            repayRecord.setCustomerId(order.getCustomerId());
            repayRecord.setOrderId(order.getId());
            repayRecord.setOrderNo(order.getOrderNo());
            repayRecord.setAmount(order.getCurrentDebt());
            repayRecord.setType("REPAID");
            repayRecord.setRemark("订单作废，回退欠款");
            debtRecordService.save(repayRecord);

            Customer customer = customerService.getById(order.getCustomerId());
            if (customer != null) {
                BigDecimal newDebt = (customer.getTotalDebt() != null ? customer.getTotalDebt() : BigDecimal.ZERO)
                        .subtract(order.getCurrentDebt());
                if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
                    newDebt = BigDecimal.ZERO;
                }
                customer.setTotalDebt(newDebt);
                customerService.updateById(customer);
            }
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
    public Result<Boolean> saveDebt(@RequestBody DebtRecord debtRecord) {
        return Result.success(debtRecordService.save(debtRecord));
    }

    @GetMapping("/debt/{customerId}")
    public Result<List<DebtRecord>> listDebtsByCustomerId(@PathVariable Long customerId) {
        return Result.success(debtRecordService.lambdaQuery()
                .eq(DebtRecord::getCustomerId, customerId)
                .orderByDesc(DebtRecord::getCreateTime)
                .list());
    }
}
