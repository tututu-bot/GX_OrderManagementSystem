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
     * 记录类型
     * 数据库字段: type， VARCHAR(20)，DEFAULT 'NEW'
     * 取值: NEW(新增欠款) / REPAID(还款)
     * 用途: 区分是新增欠款还是还款记录
     */
    private String type;

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
