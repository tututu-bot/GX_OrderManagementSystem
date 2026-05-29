package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 欠款-还款核销关联实体类
 * 对应数据库表: debt_repayment
 * 功能: 记录每笔还款具体核销了哪几笔欠款的多少金额
 *
 * 设计说明:
 * - 多对多关联表：一笔还款可以核销多笔欠款，一笔欠款可以被多笔还款核销
 * - 通过此表可精确追溯：某笔还款还了哪些订单的欠款、各还了多少
 * - 每笔核销记录的 amount 表示本次核销的金额
 */
@Data
@TableName("debt_repayment")
public class DebtRepayment {

    /**
     * 记录ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 欠款记录ID，外键关联 debt_record
     */
    private Long debtId;

    /**
     * 还款记录ID，外键关联 repayment_record
     */
    private Long repaymentId;

    /**
     * 核销金额
     * 用途: 本次核销的具体金额
     * 说明: 不能超过对应欠款的剩余未还金额
     */
    private BigDecimal amount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
