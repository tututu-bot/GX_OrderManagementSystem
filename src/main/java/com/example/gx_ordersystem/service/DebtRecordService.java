package com.example.gx_ordersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.gx_ordersystem.entity.DebtRecord;

/**
 * 欠款记录业务逻辑层接口
 * 继承 IService<DebtRecord>，MyBatis-Plus 自动提供基础 Service 方法
 *
 * IService 自动提供的方法包括:
 * - save(DebtRecord): 保存一条欠款记录
 * - saveBatch(List<DebtRecord>): 批量保存
 * - saveOrUpdate(DebtRecord): 保存或更新
 * - removeById(Long): 根据ID删除记录
 * - removeByMap(Map): 根据Map条件删除
 * - remove(Wrapper): 根据条件删除
 * - updateById(DebtRecord): 根据ID更新记录
 * - update(DebtRecord, Wrapper): 根据条件更新
 * - getById(Long): 根据ID查询记录
 * - getOne(Wrapper): 根据条件查询单条记录
 * - list(): 查询所有记录列表
 * - list(Wrapper): 根据条件查询记录列表
 * - listByIds(List): 根据ID批量查询
 * - listByMap(Map): 根据Map条件查询
 * - count(): 统计记录总数
 * - count(Wrapper): 根据条件统计
 * - page(Page, Wrapper): 分页查询
 */
public interface DebtRecordService extends IService<DebtRecord> {

    /*
     * 如需自定义业务方法，可在此声明，例如:
     *
     * List<DebtRecord> getByCustomerId(Long customerId);
     *
     * BigDecimal getTotalDebtByCustomerId(Long customerId);
     *
     * boolean createDebtRecord(DebtRecord record);
     */
}
