package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层接口
 * 继承 BaseMapper<User>，MyBatis-Plus 自动提供基础 CRUD 方法
 *
 * BaseMapper 自动提供的方法包括:
 * - insert(User): 插入一条记录
 * - deleteById(Long): 根据ID删除
 * - deleteByMap(Map): 根据Map条件删除
 * - delete(QueryWrapper): 根据条件构造器删除
 * - updateById(User): 根据ID更新
 * - update(User, Wrapper): 根据条件更新
 * - selectById(Long): 根据ID查询
 * - selectBatchIds(List): 根据ID批量查询
 * - selectByMap(Map): 根据Map条件查询
 * - selectOne(Wrapper): 根据条件查询一条
 * - selectCount(Wrapper): 根据条件统计数量
 * - selectList(Wrapper): 根据条件查询列表
 * - selectMaps(Wrapper): 根据条件查询Map列表
 * - selectPage(Page, Wrapper): 分页查询
 *
 * @Mapper 注解: 标识为MyBatis Mapper接口，由Spring扫描并注册为Bean
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /*
     * 如需自定义SQL查询，可在此添加方法，例如:
     *
     * @Select("SELECT * FROM sys_user WHERE account = #{account}")
     * User selectByAccount(@Param("account") String account);
     */
}
