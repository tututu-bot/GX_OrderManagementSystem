package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户数据访问层接口
 * 继承 BaseMapper<Customer>，MyBatis-Plus 自动提供基础 CRUD 方法
 *
 * BaseMapper 自动提供的方法包括:
 * - insert(Customer): 插入一条客户记录
 * - deleteById(Long): 根据客户ID删除
 * - delete(QueryWrapper): 根据条件删除
 * - updateById(Customer): 根据ID更新客户信息
 * - update(Customer, Wrapper): 根据条件更新
 * - selectById(Long): 根据ID查询客户
 * - selectBatchIds(List): 根据ID批量查询
 * - selectByMap(Map): 根据Map条件查询
 * - selectOne(Wrapper): 根据条件查询单个客户
 * - selectCount(Wrapper): 统计客户数量
 * - selectList(Wrapper): 查询客户列表
 * - selectMaps(Wrapper): 查询Map列表
 * - selectPage(Page, Wrapper): 分页查询客户列表
 *
 * @Mapper 注解: 标识为MyBatis Mapper接口，由Spring扫描并注册为Bean
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /*
     * 如需自定义SQL查询，可在此添加方法，例如:
     *
     * @Select("SELECT * FROM customer WHERE customer_name LIKE CONCAT('%', #{keyword}, '%') OR phone LIKE CONCAT('%', #{keyword}, '%')")
     * List<Customer> searchByKeyword(@Param("keyword") String keyword);
     */
}
