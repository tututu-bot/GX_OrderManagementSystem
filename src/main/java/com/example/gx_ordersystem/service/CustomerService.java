package com.example.gx_ordersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.gx_ordersystem.entity.Customer;

/**
 * 客户业务逻辑层接口
 * 继承 IService<Customer>，MyBatis-Plus 自动提供基础 Service 方法
 *
 * IService 自动提供的方法包括:
 * - save(Customer): 保存一条客户记录
 * - saveBatch(List<Customer>): 批量保存客户
 * - saveOrUpdate(Customer): 保存或更新
 * - removeById(Long): 根据ID删除客户
 * - removeByMap(Map): 根据Map条件删除
 * - remove(Wrapper): 根据条件删除
 * - updateById(Customer): 根据ID更新客户信息
 * - update(Customer, Wrapper): 根据条件更新
 * - getById(Long): 根据ID查询客户
 * - getOne(Wrapper): 根据条件查询单个客户
 * - list(): 查询所有客户列表
 * - list(Wrapper): 根据条件查询客户列表
 * - listByIds(List): 根据ID批量查询
 * - listByMap(Map): 根据Map条件查询
 * - count(): 统计客户总数
 * - count(Wrapper): 根据条件统计
 * - page(Page, Wrapper): 分页查询客户
 */
public interface CustomerService extends IService<Customer> {

    /*
     * 如需自定义业务方法，可在此声明，例如:
     *
     * List<Customer> searchByKeyword(String keyword);
     *
     * Customer getByPhone(String phone);
     *
     * boolean updateDebt(Long customerId, BigDecimal amount);
     */
}
