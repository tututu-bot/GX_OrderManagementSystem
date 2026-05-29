package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.RepaymentRecord;
import com.example.gx_ordersystem.mapper.RepaymentRecordMapper;
import com.example.gx_ordersystem.service.RepaymentRecordService;
import org.springframework.stereotype.Service;

/**
 * 还款记录 Service 实现类
 */
@Service
public class RepaymentRecordServiceImpl extends ServiceImpl<RepaymentRecordMapper, RepaymentRecord> implements RepaymentRecordService {
}
