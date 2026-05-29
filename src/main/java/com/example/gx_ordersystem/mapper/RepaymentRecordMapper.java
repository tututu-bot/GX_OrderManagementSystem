package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.RepaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 还款记录 Mapper 接口
 * 对应数据库表: repayment_record
 */
@Mapper
public interface RepaymentRecordMapper extends BaseMapper<RepaymentRecord> {
}
