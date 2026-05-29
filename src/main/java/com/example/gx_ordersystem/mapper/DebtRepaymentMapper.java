package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.DebtRepayment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 欠款-还款核销关联 Mapper 接口
 * 对应数据库表: debt_repayment
 */
@Mapper
public interface DebtRepaymentMapper extends BaseMapper<DebtRepayment> {
}
