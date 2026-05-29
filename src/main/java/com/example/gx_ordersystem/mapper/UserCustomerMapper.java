package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.UserCustomer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-客户关联数据访问层接口
 * 继承 BaseMapper<UserCustomer>，MyBatis-Plus 自动提供基础 CRUD 方法
 *
 * @Mapper 注解: 标识为MyBatis Mapper接口，由Spring扫描并注册为Bean
 */
@Mapper
public interface UserCustomerMapper extends BaseMapper<UserCustomer> {
}
