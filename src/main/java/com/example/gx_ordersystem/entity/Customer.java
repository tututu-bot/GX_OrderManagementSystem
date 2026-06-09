package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户实体类
 * 对应数据库表: customer
 * 功能: 存储购买商品的客户档案信息
 *
 * 注意: 客户数据为全局共享，不直接关联用户。
 * 用户与客户的关联关系通过 user_customer 表维护（多对多）。
 *
 * 主要字段说明:
 * - customerName: 客户名称，可为企业名或个人姓名
 * - category: 客户分类（零售/批发/老客户），用于区分客户类型
 * - totalDebt: 该客户当前的累计欠款金额，每次交易后自动更新
 * - status: 客户状态，1-正常，0-禁用
 */
@Data
@TableName("customer")
public class Customer {

    /**
     * 客户ID，主键，自增
     * 数据库字段: id， INTEGER
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 客户名称
     * 数据库字段: customer_name， VARCHAR(100)，NOT NULL
     * 用途: 显示在单据上的客户名称，如"佛山英子"
     */
    private String customerName;

    /**
     * 客户分类
     * 数据库字段: category， VARCHAR(20)，DEFAULT '零售'
     * 取值: 零售 / 批发 / 老客户
     * 用途: 区分客户类型，便于分类管理和统计
     */
    private String category;

    /**
     * 联系人姓名
     * 数据库字段: contact_name， VARCHAR(50)
     * 用途: 客户方的具体联系人
     */
    private String contactName;

    /**
     * 联系电话
     * 数据库字段: phone， VARCHAR(20)
     * 用途: 客户联系电话，支持按电话检索
     */
    private String phone;

    /**
     * 详细地址
     * 数据库字段: address， VARCHAR(200)
     * 用途: 客户收货地址或经营地址
     */
    private String address;

    /** 主营产品 */
    private String coreProduct;

    /** 备注 */
    private String remark;

    /**
     * 累计欠款金额
     * 数据库字段: total_debt， DECIMAL(18,2)，DEFAULT 0.00
     * 用途: 该客户所有历史交易中尚未结清的欠款总额
     */
    private BigDecimal totalDebt;

    /**
     * 客户状态
     * 数据库字段: status， INT，DEFAULT 1
     * 取值: 0-禁用，1-启用
     */
    private Integer status;

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
