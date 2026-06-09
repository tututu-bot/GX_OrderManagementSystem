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
 * productCode 格式：{颜色}{品类}-{型号}-{规格}-{用户ID}
 * specification 格式: 颜色;尺码;码数 (以;分隔)
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productCode;

    private String productName;

    private String specification;

    private String unit;

    private BigDecimal unitPrice;

    private String remark;

    /** 所属用户ID，实现数据隔离 */
    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
