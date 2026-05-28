package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.SaleOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售订单主表数据访问层接口
 * 继承 BaseMapper<SaleOrder>，MyBatis-Plus 自动提供基础 CRUD 方法
 *
 * BaseMapper 自动提供的方法包括:
 * - insert(SaleOrder): 插入一条订单记录
 * - deleteById(Long): 根据订单ID删除
 * - delete(QueryWrapper): 根据条件删除
 * - updateById(SaleOrder): 根据ID更新订单
 * - update(SaleOrder, Wrapper): 根据条件更新
 * - selectById(Long): 根据ID查询订单
 * - selectBatchIds(List): 根据ID批量查询
 * - selectByMap(Map): 根据Map条件查询
 * - selectOne(Wrapper): 根据条件查询单个订单
 * - selectCount(Wrapper): 统计订单数量
 * - selectList(Wrapper): 查询订单列表
 * - selectMaps(Wrapper): 查询Map列表
 * - selectPage(Page, Wrapper): 分页查询订单
 *
 * @Mapper 注解: 标识为MyBatis Mapper接口，由Spring扫描并注册为Bean
 */
@Mapper
public interface SaleOrderMapper extends BaseMapper<SaleOrder> {

    /*
     * 如需自定义SQL查询，可在此添加方法，例如:
     *
     * @Select("SELECT * FROM sale_order WHERE order_no = #{orderNo}")
     * SaleOrder selectByOrderNo(@Param("orderNo") String orderNo);
     *
     * @Select("SELECT * FROM sale_order WHERE customer_id = #{customerId} ORDER BY create_time DESC")
     * List<SaleOrder> selectByCustomerId(@Param("customerId") Long customerId);
     */
}
