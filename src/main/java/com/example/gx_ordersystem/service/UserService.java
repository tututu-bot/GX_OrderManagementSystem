package com.example.gx_ordersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.gx_ordersystem.entity.User;

/**
 * 用户业务逻辑层接口
 * 继承 IService<User>，MyBatis-Plus 自动提供基础 Service 方法
 *
 * IService 自动提供的方法包括:
 * - save(User): 保存一条用户记录
 * - saveBatch(List<User>): 批量保存
 * - saveOrUpdate(User): 保存或更新（根据ID判断）
 * - removeById(Long): 根据ID删除用户
 * - removeByMap(Map): 根据Map条件删除
 * - remove(Wrapper): 根据条件删除
 * - updateById(User): 根据ID更新用户信息
 * - update(User, Wrapper): 根据条件更新
 * - getById(Long): 根据ID查询用户
 * - getOne(Wrapper): 根据条件查询单个用户
 * - list(): 查询所有用户列表
 * - list(Wrapper): 根据条件查询用户列表
 * - listByIds(List): 根据ID批量查询
 * - listByMap(Map): 根据Map条件查询
 * - count(): 统计用户总数
 * - count(Wrapper): 根据条件统计
 * - page(Page, Wrapper): 分页查询用户
 */
public interface UserService extends IService<User> {

    /*
     * 如需自定义业务方法，可在此声明，例如:
     *
     * User login(String account, String password);
     *
     * boolean register(User user);
     */
}
