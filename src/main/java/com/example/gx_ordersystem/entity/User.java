package com.example.gx_ordersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表: sys_user
 * 功能: 存储系统用户（销售人员/管理员）的账号信息
 *
 * 主要字段说明:
 * - account: 登录账号，全局唯一，不可重复
 * - password: 登录密码，采用BCrypt加密存储
 * - realName: 用户真实姓名，用于单据显示
 * - status: 账号状态，1-正常启用，0-已禁用
 */
@Data
@TableName("sys_user")
public class User {

    /**
     * 用户ID，主键，自增
     * 数据库字段: id， BIGINT 类型
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号，唯一标识
     * 数据库字段: account， VARCHAR(50)，NOT NULL，UNIQUE
     * 用途: 用户登录时的用户名
     */
    private String account;

    /**
     * 登录密码，BCrypt加密存储
     * 数据库字段: password， VARCHAR(100)，NOT NULL
     * 用途: 用户登录时的密码验证
     */
    private String password;

    /**
     * 用户真实姓名
     * 数据库字段: real_name， VARCHAR(50)
     * 用途: 单据上显示的制单人/销售人员姓名
     */
    private String realName;

    /**
     * 用户地址
     * 数据库字段: address， VARCHAR(200)
     * 用途: 记录用户住址或办公地址
     */
    private String address;

    /**
     * 联系方式（手机号码）
     * 数据库字段: phone， VARCHAR(20)
     * 用途: 用户联系电话
     */
    private String phone;

    /**
     * 其他信息（扩展字段）
     * 数据库字段: extra_info， VARCHAR(500)
     * 用途: 存储用户的其他补充信息
     */
    private String extraInfo;

    /**
     * 账号状态
     * 数据库字段: status， INT，DEFAULT 1
     * 取值: 0-禁用，1-启用
     * 用途: 控制账号是否可用
     */
    private Integer status;

    /**
     * 创建时间
     * 数据库字段: create_time， DATETIME，DEFAULT CURRENT_TIMESTAMP
     * 用途: 记录账号注册时间，自动生成
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 数据库字段: update_time， DATETIME，自动更新
     * 用途: 记录最后一次修改时间，自动更新
     */
    private LocalDateTime updateTime;
}
