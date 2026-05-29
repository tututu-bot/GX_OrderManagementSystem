package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 欠款记录实体类
 * 对应数据库表: debt_record
 * 功能: 记录客户的每一笔欠款或还款明细
 *
 * 说明:
 * - 当产生新欠款时，type = "NEW"，amount 为正数
 * - 当客户还款时，type = "REPAID"，amount 为正数
 * - 通过 userId 实现数据隔离：每个用户只能看到自己的欠款记录
 * - 通过 orderId 和 orderNo 关联到具体订单
 * - 通过 customerId 关联到客户
 */
@Data
@TableName("debt_record")
public class DebtRecord {

    /**
     * 记录ID，主键，自增
     * 数据库字段: id， BIGINT
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，外键关联 sys_user 表
     * 数据库字段: user_id， INTEGER，NOT NULL
     * 用途: 实现欠款数据隔离，每个用户的欠款独立计算
     */
    private Long userId;

    /**
     * 客户ID，外键
     * 数据库字段: customer_id， BIGINT，NOT NULL
     * 关联表: customer(id)
     * 用途: 标识该欠款记录属于哪个客户
     */
    private Long customerId;

    /**
     * 关联订单ID，外键
     * 数据库字段: order_id， BIGINT
     * 关联表: sale_order(id)
     * 用途: 标识该欠款记录关联的订单
     */
    private Long orderId;

    /**
     * 关联单据编号
     * 数据库字段: order_no， VARCHAR(30)
     * 用途: 冗余存储单据编号，方便直接查看
     */
    private String orderNo;

    /**
     * 欠款金额
     * 数据库字段: amount， DECIMAL(18,2)，NOT NULL
     * 用途: 欠款的金额，始终为正数
     */
    private BigDecimal amount;

    /**
     * 已还金额
     * 数据库字段: settled_amount， DECIMAL(18,2)，DEFAULT 0.00
     * 用途: 该笔欠款已经被还款的金额
     * 说明: settled_amount = amount 时表示已全额还清
     */
    private BigDecimal settledAmount;

    /**
     * 欠款状态
     * 数据库字段: status， VARCHAR(20)，DEFAULT 'UNSETTLED'
     * 取值: UNSETTLED(未结清) / PARTIAL(部分结清) / SETTLED(已结清)
     * 用途: 标识该欠款记录的结清状态
     */
    private String status;

    /**
     * 备注
     * 数据库字段: remark， VARCHAR(500)
     * 用途: 欠款记录的备注说明
     */
    private String remark;

    /**
     * 创建时间，自动记录
     * 数据库字段: create_time， DATETIME
     */
    private LocalDateTime createTime;
}
