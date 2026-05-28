package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.SaleOrder;
import com.example.gx_ordersystem.mapper.SaleOrderMapper;
import com.example.gx_ordersystem.service.SaleOrderService;
import org.springframework.stereotype.Service;

/**
 * 销售订单业务逻辑层实现类
 * 继承 ServiceImpl<SaleOrderMapper, SaleOrder>，自动注入 SaleOrderMapper 并实现 IService 的所有基础方法
 *
 * @Service 注解: 标识为Spring的Service组件，由Spring容器管理生命周期和依赖注入
 *
 * 继承的方法（自动可用，无需手动实现）:
 * - save(SaleOrder): 保存订单
 * - removeById(Long): 根据ID删除订单
 * - updateById(SaleOrder): 根据ID更新订单
 * - getById(Long): 根据ID查询订单
 * - list(): 查询所有订单
 * - count(): 统计订单数量
 * - page(Page, Wrapper): 分页查询订单
 *
 * 如需添加自定义业务方法（如订单创建、金额计算、编号生成等），可在此类中添加具体实现
 */
@Service
public class SaleOrderServiceImpl extends ServiceImpl<SaleOrderMapper, SaleOrder> implements SaleOrderService {
}
