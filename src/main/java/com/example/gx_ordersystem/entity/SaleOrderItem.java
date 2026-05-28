package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单明细实体类
 * 对应数据库表: sale_order_item
 * 功能: 存储销售订单中的每一项商品明细
 *
 * 说明: 一个销售订单(sale_order)可以包含多个订单明细(sale_order_item)
 * 通过 orderId 字段关联到所属订单
 */
@Data
@TableName("sale_order_item")
public class SaleOrderItem {

    /**
     * 明细ID，主键，自增
     * 数据库字段: id， BIGINT
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属订单ID，外键
     * 数据库字段: order_id， BIGINT，NOT NULL
     * 关联表: sale_order(id)
     * 用途: 标识该明细属于哪个订单
     */
    private Long orderId;

    /**
     * 序号
     * 数据库字段: seq_no， INT，DEFAULT 1
     * 用途: 商品在单据中的显示顺序（1, 2, 3...）
     */
    private Integer seqNo;

    /**
     * 商品编号
     * 数据库字段: product_code， VARCHAR(50)
     * 用途: 商品的唯一编码，如"SP1778550650"
     */
    private String productCode;

    /**
     * 商品名称
     * 数据库字段: product_name， VARCHAR(200)，NOT NULL
     * 用途: 商品的中文名称，如"加厚白针织37#预缩"
     */
    private String productName;

    /**
     * 规格型号
     * 数据库字段: specification， VARCHAR(100)
     * 用途: 商品的规格信息
     */
    private String specification;

    /**
     * 单位
     * 数据库字段: unit， VARCHAR(20)
     * 用途: 商品计量单位，如"公斤"、"米"、"件"
     */
    private String unit;

    /**
     * 数量
     * 数据库字段: quantity， DECIMAL(18,3)，DEFAULT 0.000
     * 用途: 购买的数量，支持小数（如15.4公斤）
     */
    private BigDecimal quantity;

    /**
     * 单价
     * 数据库字段: unit_price， DECIMAL(18,2)，DEFAULT 0.00
     * 用途: 商品的单价（每单位价格）
     */
    private BigDecimal unitPrice;

    /**
     * 金额
     * 数据库字段: amount， DECIMAL(18,2)，DEFAULT 0.00
     * 计算: quantity × unit_price
     * 用途: 该项商品的总金额
     */
    private BigDecimal amount;

    /**
     * 备注
     * 数据库字段: remark， VARCHAR(200)
     * 用途: 该商品项的备注，如规格"4.0cm"
     */
    private String remark;

    /**
     * 创建时间，自动记录
     * 数据库字段: create_time， DATETIME
     */
    private LocalDateTime createTime;
}
