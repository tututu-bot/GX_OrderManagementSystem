package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.User;
import com.example.gx_ordersystem.mapper.UserMapper;
import com.example.gx_ordersystem.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户业务逻辑层实现类
 * 继承 ServiceImpl<UserMapper, User>，自动注入 UserMapper 并实现 IService 的所有基础方法
 *
 * @Service 注解: 标识为Spring的Service组件，由Spring容器管理生命周期和依赖注入
 *
 * 继承的方法（自动可用，无需手动实现）:
 * - save(User): 保存用户
 * - removeById(Long): 根据ID删除用户
 * - updateById(User): 根据ID更新用户
 * - getById(Long): 根据ID查询用户
 * - list(): 查询所有用户
 * - count(): 统计用户数量
 * - page(Page, Wrapper): 分页查询用户
 *
 * 如需添加自定义业务方法，可在此类中添加具体实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
