package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.DebtRepayment;
import com.example.gx_ordersystem.mapper.DebtRepaymentMapper;
import com.example.gx_ordersystem.service.DebtRepaymentService;
import org.springframework.stereotype.Service;

/**
 * 欠款-还款核销关联 Service 实现类
 */
@Service
public class DebtRepaymentServiceImpl extends ServiceImpl<DebtRepaymentMapper, DebtRepayment> implements DebtRepaymentService {
}
