package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 对应数据库表: product
 * 功能: 存储商品基础信息，作为订单明细的商品来源
 *
 * 主要字段说明:
 * - productName: 商品名称，如"白针织42#"，唯一标识一个商品
 * - specification: 规格型号，如"1.9mm/6分"
 * - unit: 计量单位，默认"公斤"
 * - unitPrice: 标准单价
 * - remark: 备注信息
 */
@Data
@TableName("product")
public class Product {

    /**
     * 商品ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品编码，唯一标识
     * 格式: {颜色首字母}{品类}{型号}-{规格数字}
     * 例: BZZ-42-19 (白针织42# 1.9mm)
     * 数据库字段: product_code， VARCHAR(50)，NOT NULL，UNIQUE
     */
    private String productCode;

    /**
     * 商品名称
     * 数据库字段: product_name， VARCHAR(200)，NOT NULL
     */
    private String productName;

    /**
     * 规格型号
     * 数据库字段: specification， VARCHAR(100)
     */
    private String specification;

    /**
     * 计量单位
     * 数据库字段: unit， VARCHAR(20)，DEFAULT '公斤'
     */
    private String unit;

    /**
     * 标准单价
     * 数据库字段: unit_price， DECIMAL(18,2)
     */
    private BigDecimal unitPrice;

    /**
     * 备注
     * 数据库字段: remark， VARCHAR(200)
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
