package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 还款记录实体类
 * 对应数据库表: repayment_record
 * 功能: 记录客户的每一笔独立还款
 *
 * 设计说明:
 * - 还款是独立的业务事件，与订单解耦
 * - 客户可以随时还款，不一定跟某张订单的修改挂钩
 * - 一笔还款可以核销多笔欠款（通过 debt_repayment 关联表）
 * - 通过 user_id 实现数据隔离
 */
@Data
@TableName("repayment_record")
public class RepaymentRecord {

    /**
     * 记录ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，外键
     * 用途: 实现还款数据隔离
     */
    private Long userId;

    /**
     * 客户ID，外键
     * 用途: 标识该还款属于哪个客户
     */
    private Long customerId;

    /**
     * 还款金额
     * 用途: 客户本次实际还款的金额，始终为正数
     */
    private BigDecimal amount;

    /**
     * 还款方式
     * 取值: 现金 / 微信 / 支付宝 / 银行转账
     */
    private String paymentMethod;

    /**
     * 还款日期
     * 用途: 客户实际还款的日期
     */
    private LocalDate repaymentDate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
