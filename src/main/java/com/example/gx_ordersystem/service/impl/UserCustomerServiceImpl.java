package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.UserCustomer;
import com.example.gx_ordersystem.mapper.UserCustomerMapper;
import com.example.gx_ordersystem.service.UserCustomerService;
import org.springframework.stereotype.Service;

/**
 * 用户-客户关联业务逻辑层实现类
 * 继承 ServiceImpl<UserCustomerMapper, UserCustomer>，自动注入 UserCustomerMapper
 *
 * @Service 注解: 标识为Spring的Service组件，由Spring容器管理
 */
@Service
public class UserCustomerServiceImpl extends ServiceImpl<UserCustomerMapper, UserCustomer> implements UserCustomerService {
}
