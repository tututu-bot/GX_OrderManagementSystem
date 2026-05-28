package com.example.gx_ordersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.gx_ordersystem.entity.SaleOrder;

/**
 * 销售订单业务逻辑层接口
 * 继承 IService<SaleOrder>，MyBatis-Plus 自动提供基础 Service 方法
 *
 * IService 自动提供的方法包括:
 * - save(SaleOrder): 保存一条订单记录
 * - saveBatch(List<SaleOrder>): 批量保存订单
 * - saveOrUpdate(SaleOrder): 保存或更新
 * - removeById(Long): 根据ID删除订单
 * - removeByMap(Map): 根据Map条件删除
 * - remove(Wrapper): 根据条件删除
 * - updateById(SaleOrder): 根据ID更新订单
 * - update(SaleOrder, Wrapper): 根据条件更新
 * - getById(Long): 根据ID查询订单
 * - getOne(Wrapper): 根据条件查询单个订单
 * - list(): 查询所有订单列表
 * - list(Wrapper): 根据条件查询订单列表
 * - listByIds(List): 根据ID批量查询
 * - listByMap(Map): 根据Map条件查询
 * - count(): 统计订单总数
 * - count(Wrapper): 根据条件统计
 * - page(Page, Wrapper): 分页查询订单
 */
public interface SaleOrderService extends IService<SaleOrder> {

    /*
     * 如需自定义业务方法，可在此声明，例如:
     *
     * SaleOrder createOrder(OrderDTO dto);
     *
     * boolean cancelOrder(Long orderId);
     *
     * String generateOrderNo();
     */
}
