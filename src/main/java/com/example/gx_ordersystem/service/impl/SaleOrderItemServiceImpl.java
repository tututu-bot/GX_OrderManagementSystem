package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.SaleOrderItem;
import com.example.gx_ordersystem.mapper.SaleOrderItemMapper;
import com.example.gx_ordersystem.service.SaleOrderItemService;
import org.springframework.stereotype.Service;

/**
 * 销售订单明细业务逻辑层实现类
 * 继承 ServiceImpl<SaleOrderItemMapper, SaleOrderItem>，自动注入 SaleOrderItemMapper 并实现 IService 的所有基础方法
 *
 * @Service 注解: 标识为Spring的Service组件，由Spring容器管理生命周期和依赖注入
 *
 * 继承的方法（自动可用，无需手动实现）:
 * - save(SaleOrderItem): 保存订单明细
 * - removeById(Long): 根据ID删除明细
 * - updateById(SaleOrderItem): 根据ID更新明细
 * - getById(Long): 根据ID查询明细
 * - list(): 查询所有明细
 * - count(): 统计明细数量
 * - page(Page, Wrapper): 分页查询明细
 *
 * 如需添加自定义业务方法（如按订单查询明细、批量保存明细等），可在此类中添加具体实现
 */
@Service
public class SaleOrderItemServiceImpl extends ServiceImpl<SaleOrderItemMapper, SaleOrderItem> implements SaleOrderItemService {
}
