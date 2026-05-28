package com.example.gx_ordersystem.controller;

import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.DebtRecord;
import com.example.gx_ordersystem.entity.SaleOrder;
import com.example.gx_ordersystem.entity.SaleOrderItem;
import com.example.gx_ordersystem.service.DebtRecordService;
import com.example.gx_ordersystem.service.SaleOrderItemService;
import com.example.gx_ordersystem.service.SaleOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单管理控制器
 * 处理销售订单相关的HTTP请求，提供订单、订单明细、欠款记录的增删改查RESTful API
 *
 * 基础请求路径: /api/order
 *
 * @RestController 注解: 标识为Spring MVC控制器，自动将返回值转换为JSON格式
 * @RequestMapping("/api/order") 注解: 定义该控制器的根路径
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    /**
     * 销售订单业务逻辑层接口
     * 处理订单主表的数据操作
     */
    @Autowired
    private SaleOrderService saleOrderService;

    /**
     * 销售订单明细业务逻辑层接口
     * 处理订单明细表的数据操作
     */
    @Autowired
    private SaleOrderItemService saleOrderItemService;

    /**
     * 欠款记录业务逻辑层接口
     * 处理欠款记录表的数据操作
     */
    @Autowired
    private DebtRecordService debtRecordService;

    // ========================== 订单主表接口 ==========================

    /**
     * 新增销售订单
     * 请求方式: POST
     * 请求路径: /api/order
     * 请求体: SaleOrder对象的JSON格式
     *
     * @param order 订单对象（包含orderNo, customerId, salesPerson, orderDate等字段）
     * @return Result<Boolean> 保存成功返回true
     *
     * 示例请求:
     * POST /api/order
     * {
     *   "orderNo": "XSDD202605161201439",
     *   "customerId": 1,
     *   "salesPerson": "李光利",
     *   "orderDate": "2026-05-16",
     *   "deliveryDate": "2026-05-16",
     *   "totalAmount": 215.60,
     *   "paymentMethod": "赊账"
     * }
     */
    @PostMapping
    public Result<Boolean> saveOrder(@RequestBody SaleOrder order) {
        return Result.success(saleOrderService.save(order));
    }

    /**
     * 删除销售订单
     * 请求方式: DELETE
     * 请求路径: /api/order/{id}
     * 路径参数: id - 订单ID
     *
     * @param id 订单ID
     * @return Result<Boolean> 删除成功返回true
     *
     * 示例请求: DELETE /api/order/1
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteOrder(@PathVariable Long id) {
        return Result.success(saleOrderService.removeById(id));
    }

    /**
     * 修改销售订单
     * 请求方式: PUT
     * 请求路径: /api/order
     * 请求体: SaleOrder对象的JSON格式（必须包含id字段）
     *
     * @param order 订单对象（包含需要更新的字段）
     * @return Result<Boolean> 修改成功返回true
     */
    @PutMapping
    public Result<Boolean> updateOrder(@RequestBody SaleOrder order) {
        return Result.success(saleOrderService.updateById(order));
    }

    /**
     * 根据ID查询订单
     * 请求方式: GET
     * 请求路径: /api/order/{id}
     * 路径参数: id - 订单ID
     *
     * @param id 订单ID
     * @return Result<SaleOrder> 订单详情对象
     *
     * 示例请求: GET /api/order/1
     */
    @GetMapping("/{id}")
    public Result<SaleOrder> getOrderById(@PathVariable Long id) {
        return Result.success(saleOrderService.getById(id));
    }

    /**
     * 查询全部订单列表
     * 请求方式: GET
     * 请求路径: /api/order
     *
     * @return Result<List<SaleOrder>> 订单列表
     *
     * 示例请求: GET /api/order
     */
    @GetMapping
    public Result<List<SaleOrder>> listOrders() {
        return Result.success(saleOrderService.list());
    }

    // ========================== 订单明细接口 ==========================

    /**
     * 新增订单明细
     * 请求方式: POST
     * 请求路径: /api/order/item
     * 请求体: SaleOrderItem对象的JSON格式
     *
     * @param item 订单明细对象（包含orderId, productName, quantity, unitPrice等字段）
     * @return Result<Boolean> 保存成功返回true
     *
     * 示例请求:
     * POST /api/order/item
     * {
     *   "orderId": 1,
     *   "seqNo": 1,
     *   "productCode": "SP1778550650",
     *   "productName": "加厚白针织37#预缩",
     *   "unit": "公斤",
     *   "quantity": 15.4,
     *   "unitPrice": 14.00,
     *   "remark": "4.0cm"
     * }
     */
    @PostMapping("/item")
    public Result<Boolean> saveItem(@RequestBody SaleOrderItem item) {
        return Result.success(saleOrderItemService.save(item));
    }

    /**
     * 删除订单明细
     * 请求方式: DELETE
     * 请求路径: /api/order/item/{id}
     * 路径参数: id - 明细ID
     *
     * @param id 订单明细ID
     * @return Result<Boolean> 删除成功返回true
     *
     * 示例请求: DELETE /api/order/item/1
     */
    @DeleteMapping("/item/{id}")
    public Result<Boolean> deleteItem(@PathVariable Long id) {
        return Result.success(saleOrderItemService.removeById(id));
    }

    /**
     * 根据订单ID查询明细列表
     * 请求方式: GET
     * 请求路径: /api/order/item/{orderId}
     * 路径参数: orderId - 所属订单ID
     *
     * @param orderId 订单ID
     * @return Result<List<SaleOrderItem>> 该订单下的所有明细项
     *
     * 示例请求: GET /api/order/item/1
     */
    @GetMapping("/item/{orderId}")
    public Result<List<SaleOrderItem>> listItemsByOrderId(@PathVariable Long orderId) {
        return Result.success(saleOrderItemService.lambdaQuery().eq(SaleOrderItem::getOrderId, orderId).list());
    }

    // ========================== 欠款记录接口 ==========================

    /**
     * 新增欠款记录
     * 请求方式: POST
     * 请求路径: /api/order/debt
     * 请求体: DebtRecord对象的JSON格式
     *
     * @param debtRecord 欠款记录对象（包含customerId, orderId, amount, type等字段）
     * @return Result<Boolean> 保存成功返回true
     *
     * 示例请求:
     * POST /api/order/debt
     * {
     *   "customerId": 1,
     *   "orderId": 1,
     *   "orderNo": "XSDD202605161201439",
     *   "amount": 215.60,
     *   "type": "NEW",
     *   "remark": "赊账"
     * }
     */
    @PostMapping("/debt")
    public Result<Boolean> saveDebt(@RequestBody DebtRecord debtRecord) {
        return Result.success(debtRecordService.save(debtRecord));
    }

    /**
     * 根据客户ID查询欠款记录
     * 请求方式: GET
     * 请求路径: /api/order/debt/{customerId}
     * 路径参数: customerId - 客户ID
     *
     * @param customerId 客户ID
     * @return Result<List<DebtRecord>> 该客户的所有欠款记录
     *
     * 示例请求: GET /api/order/debt/1
     */
    @GetMapping("/debt/{customerId}")
    public Result<List<DebtRecord>> listDebtsByCustomerId(@PathVariable Long customerId) {
        return Result.success(debtRecordService.lambdaQuery().eq(DebtRecord::getCustomerId, customerId).list());
    }
}
