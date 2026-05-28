package com.example.gx_ordersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.gx_ordersystem.entity.SaleOrderItem;

/**
 * 销售订单明细业务逻辑层接口
 * 继承 IService<SaleOrderItem>，MyBatis-Plus 自动提供基础 Service 方法
 *
 * IService 自动提供的方法包括:
 * - save(SaleOrderItem): 保存一条订单明细
 * - saveBatch(List<SaleOrderItem>): 批量保存明细
 * - saveOrUpdate(SaleOrderItem): 保存或更新
 * - removeById(Long): 根据ID删除明细
 * - removeByMap(Map): 根据Map条件删除
 * - remove(Wrapper): 根据条件删除
 * - updateById(SaleOrderItem): 根据ID更新明细
 * - update(SaleOrderItem, Wrapper): 根据条件更新
 * - getById(Long): 根据ID查询明细
 * - getOne(Wrapper): 根据条件查询单条明细
 * - list(): 查询所有明细列表
 * - list(Wrapper): 根据条件查询明细列表
 * - listByIds(List): 根据ID批量查询
 * - listByMap(Map): 根据Map条件查询
 * - count(): 统计明细总数
 * - count(Wrapper): 根据条件统计
 * - page(Page, Wrapper): 分页查询明细
 */
public interface SaleOrderItemService extends IService<SaleOrderItem> {

    /*
     * 如需自定义业务方法，可在此声明，例如:
     *
     * List<SaleOrderItem> getItemsByOrderId(Long orderId);
     *
     * boolean saveItems(Long orderId, List<SaleOrderItem> items);
     */
}
