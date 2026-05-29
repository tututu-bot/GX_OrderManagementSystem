package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户-客户关联实体类
 * 对应数据库表: user_customer
 * 功能: 维护用户与客户之间的多对多关联关系，并记录用户视角下该客户的欠款金额
 *
 * 设计说明:
 * - 一个用户可以关联多个客户
 * - 一个客户可以被多个用户关联
 * - (user_id, customer_id) 组合唯一，防止重复关联
 * - debtAmount: 该用户视角下此客户的累计欠款，每个用户独立计算
 */
@Data
@TableName("user_customer")
public class UserCustomer {

    /**
     * 关联ID，主键，自增
     * 数据库字段: id， INTEGER
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，外键关联 sys_user 表
     * 数据库字段: user_id， INTEGER，NOT NULL
     */
    private Long userId;

    /**
     * 客户ID，外键关联 customer 表
     * 数据库字段: customer_id， INTEGER，NOT NULL
     */
    private Long customerId;

    /**
     * 欠款金额
     * 数据库字段: debt_amount， DECIMAL(18,2)，DEFAULT 0.00
     * 用途: 该用户视角下此客户的累计欠款金额，每个用户独立计算
     */
    private BigDecimal debtAmount;

    /**
     * 关联创建时间，自动记录
     * 数据库字段: create_time， DATETIME
     */
    private LocalDateTime createTime;
}
