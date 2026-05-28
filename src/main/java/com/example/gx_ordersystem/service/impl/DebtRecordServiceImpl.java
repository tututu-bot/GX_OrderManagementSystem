package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.DebtRecord;
import com.example.gx_ordersystem.mapper.DebtRecordMapper;
import com.example.gx_ordersystem.service.DebtRecordService;
import org.springframework.stereotype.Service;

/**
 * 欠款记录业务逻辑层实现类
 * 继承 ServiceImpl<DebtRecordMapper, DebtRecord>，自动注入 DebtRecordMapper 并实现 IService 的所有基础方法
 *
 * @Service 注解: 标识为Spring的Service组件，由Spring容器管理生命周期和依赖注入
 *
 * 继承的方法（自动可用，无需手动实现）:
 * - save(DebtRecord): 保存欠款记录
 * - removeById(Long): 根据ID删除记录
 * - updateById(DebtRecord): 根据ID更新记录
 * - getById(Long): 根据ID查询记录
 * - list(): 查询所有记录
 * - count(): 统计记录数量
 * - page(Page, Wrapper): 分页查询
 *
 * 如需添加自定义业务方法（如按客户查询欠款、欠款统计等），可在此类中添加具体实现
 */
@Service
public class DebtRecordServiceImpl extends ServiceImpl<DebtRecordMapper, DebtRecord> implements DebtRecordService {
}
