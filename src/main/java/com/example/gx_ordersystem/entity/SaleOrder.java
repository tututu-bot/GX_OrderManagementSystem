package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单主表实体类
 * 对应数据库表: sale_order
 * 功能: 存储销售订单的头部信息（单据主信息）
 *
 * 主要字段说明:
 * - orderNo: 自动生成的唯一单据编号，格式: XSDD + 日期时间 + 毫秒
 * - customerId: 关联的客户ID，外键关联customer表
 * - totalAmount: 本单所有商品金额合计（自动计算）
 * - currentDebt: 本次交易产生的欠款金额
 * - totalDebt: 交易后客户的累计欠款总额
 */
@Data
@TableName("sale_order")
public class SaleOrder {

    /**
     * 单据ID，主键，自增
     * 数据库字段: id， BIGINT
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 单据编号，全局唯一
     * 数据库字段: order_no， VARCHAR(30)，NOT NULL，UNIQUE
     * 格式: XSDD + YYYYMMDD + HHMM + SSS（毫秒）
     * 示例: XSDD202605161201439
     * 用途: 单据的唯一标识，打印在纸质单据上
     */
    private String orderNo;

    /**
     * 客户ID，外键
     * 数据库字段: customer_id， BIGINT，NOT NULL
     * 关联表: customer(id)
     * 用途: 标识该订单属于哪个客户
     */
    private Long customerId;

    /**
     * 销售人员/制单人
     * 数据库字段: sales_person， VARCHAR(50)
     * 用途: 记录开单操作人员姓名
     */
    private String salesPerson;

    /**
     * 单据日期
     * 数据库字段: order_date， DATE
     * 用途: 订单开具的日期
     */
    private LocalDate orderDate;

    /**
     * 交货日期
     * 数据库字段: delivery_date， DATE
     * 用途: 约定的交货/送货日期
     */
    private LocalDate deliveryDate;

    /**
     * 单据状态
     * 数据库字段: status， VARCHAR(20)，DEFAULT '进行中'
     * 取值: 进行中 / 已完成 / 作废
     */
    private String status;

    /**
     * 本单金额（商品合计）
     * 数据库字段: total_amount， DECIMAL(18,2)，DEFAULT 0.00
     * 计算: 所有明细项 amount 的求和
     * 用途: 本单商品的总金额
     */
    private BigDecimal totalAmount;

    /**
     * 其他费用
     * 数据库字段: other_fee， DECIMAL(18,2)，DEFAULT 0.00
     * 用途: 运费、手续费等其他附加费用
     */
    private BigDecimal otherFee;

    /**
     * 应收金额
     * 数据库字段: receivable_amount， DECIMAL(18,2)，DEFAULT 0.00
     * 计算: totalAmount + otherFee
     * 用途: 客户实际需要支付的总金额
     */
    private BigDecimal receivableAmount;

    /**
     * 本次收款
     * 数据库字段: received_amount， DECIMAL(18,2)，DEFAULT 0.00
     * 用途: 客户本次实际支付的金额
     */
    private BigDecimal receivedAmount;

    /**
     * 本次欠款
     * 数据库字段: current_debt， DECIMAL(18,2)，DEFAULT 0.00
     * 计算: receivableAmount - receivedAmount
     * 用途: 本次交易中客户未支付的欠款金额
     */
    private BigDecimal currentDebt;

    /**
     * 累计欠款
     * 数据库字段: total_debt， DECIMAL(18,2)，DEFAULT 0.00
     * 用途: 交易后客户的累计欠款总额（含历史欠款）
     */
    private BigDecimal totalDebt;

    /**
     * 收款方式
     * 数据库字段: payment_method， VARCHAR(30)
     * 取值: 现金 / 微信 / 支付宝 / 银行转账 / 赊账 / 预收定金
     */
    private String paymentMethod;

    /**
     * 备注
     * 数据库字段: remark， VARCHAR(1000)
     * 用途: 订单的备注说明
     */
    private String remark;

    /**
     * 创建时间，自动记录
     * 数据库字段: create_time， DATETIME
     */
    private LocalDateTime createTime;

    /**
     * 更新时间，自动记录
     * 数据库字段: update_time， DATETIME
     */
    private LocalDateTime updateTime;
}
