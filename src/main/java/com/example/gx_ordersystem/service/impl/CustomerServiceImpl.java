package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.Customer;
import com.example.gx_ordersystem.mapper.CustomerMapper;
import com.example.gx_ordersystem.service.CustomerService;
import org.springframework.stereotype.Service;

/**
 * 客户业务逻辑层实现类
 * 继承 ServiceImpl<CustomerMapper, Customer>，自动注入 CustomerMapper 并实现 IService 的所有基础方法
 *
 * @Service 注解: 标识为Spring的Service组件，由Spring容器管理生命周期和依赖注入
 *
 * 继承的方法（自动可用，无需手动实现）:
 * - save(Customer): 保存客户
 * - removeById(Long): 根据ID删除客户
 * - updateById(Customer): 根据ID更新客户
 * - getById(Long): 根据ID查询客户
 * - list(): 查询所有客户
 * - count(): 统计客户数量
 * - page(Page, Wrapper): 分页查询客户
 *
 * 如需添加自定义业务方法（如客户检索、欠款统计等），可在此类中添加具体实现
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {
}
