package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.SaleOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售订单明细数据访问层接口
 * 继承 BaseMapper<SaleOrderItem>，MyBatis-Plus 自动提供基础 CRUD 方法
 *
 * BaseMapper 自动提供的方法包括:
 * - insert(SaleOrderItem): 插入一条订单明细
 * - deleteById(Long): 根据明细ID删除
 * - delete(QueryWrapper): 根据条件删除
 * - updateById(SaleOrderItem): 根据ID更新明细
 * - update(SaleOrderItem, Wrapper): 根据条件更新
 * - selectById(Long): 根据ID查询明细
 * - selectBatchIds(List): 根据ID批量查询
 * - selectByMap(Map): 根据Map条件查询
 * - selectOne(Wrapper): 根据条件查询单条明细
 * - selectCount(Wrapper): 统计明细数量
 * - selectList(Wrapper): 查询明细列表
 * - selectMaps(Wrapper): 查询Map列表
 * - selectPage(Page, Wrapper): 分页查询明细
 *
 * @Mapper 注解: 标识为MyBatis Mapper接口，由Spring扫描并注册为Bean
 */
@Mapper
public interface SaleOrderItemMapper extends BaseMapper<SaleOrderItem> {

    /*
     * 如需自定义SQL查询，可在此添加方法，例如:
     *
     * @Select("SELECT * FROM sale_order_item WHERE order_id = #{orderId} ORDER BY seq_no")
     * List<SaleOrderItem> selectByOrderId(@Param("orderId") Long orderId);
     */
}
