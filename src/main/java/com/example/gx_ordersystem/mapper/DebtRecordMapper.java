package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.DebtRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 欠款记录数据访问层接口
 * 继承 BaseMapper<DebtRecord>，MyBatis-Plus 自动提供基础 CRUD 方法
 *
 * BaseMapper 自动提供的方法包括:
 * - insert(DebtRecord): 插入一条欠款记录
 * - deleteById(Long): 根据记录ID删除
 * - delete(QueryWrapper): 根据条件删除
 * - updateById(DebtRecord): 根据ID更新
 * - update(DebtRecord, Wrapper): 根据条件更新
 * - selectById(Long): 根据ID查询
 * - selectBatchIds(List): 根据ID批量查询
 * - selectByMap(Map): 根据Map条件查询
 * - selectOne(Wrapper): 根据条件查询单条
 * - selectCount(Wrapper): 统计记录数量
 * - selectList(Wrapper): 查询记录列表
 * - selectMaps(Wrapper): 查询Map列表
 * - selectPage(Page, Wrapper): 分页查询
 *
 * @Mapper 注解: 标识为MyBatis Mapper接口，由Spring扫描并注册为Bean
 */
@Mapper
public interface DebtRecordMapper extends BaseMapper<DebtRecord> {

    /*
     * 如需自定义SQL查询，可在此添加方法，例如:
     *
     * @Select("SELECT * FROM debt_record WHERE customer_id = #{customerId} ORDER BY create_time DESC")
     * List<DebtRecord> selectByCustomerId(@Param("customerId") Long customerId);
     *
     * @Select("SELECT SUM(amount) FROM debt_record WHERE customer_id = #{customerId} AND type = 'NEW'")
     * BigDecimal sumDebtByCustomerId(@Param("customerId") Long customerId);
     */
}
