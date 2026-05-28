CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    account         VARCHAR(50) NOT NULL UNIQUE COMMENT '账号（唯一）',
    password        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50) COMMENT '用户姓名',
    address         VARCHAR(200) COMMENT '地址',
    phone           VARCHAR(20) COMMENT '联系方式',
    extra_info      VARCHAR(500) COMMENT '其他信息',
    status          INT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_account (account),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS customer (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户ID',
    customer_name   VARCHAR(100) NOT NULL COMMENT '客户名称',
    category        VARCHAR(20) DEFAULT '零售' COMMENT '客户分类: 零售/批发/老客户',
    contact_name    VARCHAR(50) COMMENT '联系人',
    phone           VARCHAR(20) COMMENT '联系电话',
    address         VARCHAR(200) COMMENT '地址',
    remark          VARCHAR(500) COMMENT '备注标签',
    total_debt      DECIMAL(18,2) DEFAULT 0.00 COMMENT '累计欠款',
    status          INT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name (customer_name),
    INDEX idx_phone (phone),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='客户表';

CREATE TABLE IF NOT EXISTS sale_order (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '单据ID',
    order_no            VARCHAR(30) NOT NULL UNIQUE COMMENT '单据编号 XSDD前缀',
    customer_id         BIGINT NOT NULL COMMENT '客户ID',
    sales_person        VARCHAR(50) COMMENT '销售人员/制单人',
    order_date          DATE COMMENT '单据日期',
    delivery_date       DATE COMMENT '交货日期',
    status              VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/CONFIRMED/COMPLETED/CANCELLED',
    total_amount        DECIMAL(18,2) DEFAULT 0.00 COMMENT '本单金额',
    other_fee           DECIMAL(18,2) DEFAULT 0.00 COMMENT '其他费用',
    receivable_amount   DECIMAL(18,2) DEFAULT 0.00 COMMENT '应收金额',
    received_amount     DECIMAL(18,2) DEFAULT 0.00 COMMENT '本次收款',
    current_debt        DECIMAL(18,2) DEFAULT 0.00 COMMENT '本次欠款',
    total_debt          DECIMAL(18,2) DEFAULT 0.00 COMMENT '累计欠款',
    payment_method      VARCHAR(30) COMMENT '收款方式',
    remark              VARCHAR(1000) COMMENT '备注',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order_no (order_no),
    INDEX idx_customer (customer_id),
    INDEX idx_order_date (order_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='销售订单主表';

CREATE TABLE IF NOT EXISTS sale_order_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    order_id        BIGINT NOT NULL COMMENT '所属订单ID',
    seq_no          INT DEFAULT 1 COMMENT '序号',
    product_code    VARCHAR(50) COMMENT '商品编号',
    product_name    VARCHAR(200) NOT NULL COMMENT '商品名称',
    specification   VARCHAR(100) COMMENT '规格型号',
    unit            VARCHAR(20) COMMENT '单位',
    quantity        DECIMAL(18,3) DEFAULT 0.000 COMMENT '数量',
    unit_price      DECIMAL(18,2) DEFAULT 0.00 COMMENT '单价',
    amount          DECIMAL(18,2) DEFAULT 0.00 COMMENT '金额',
    remark          VARCHAR(200) COMMENT '备注',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (order_id) REFERENCES sale_order(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='销售订单明细表';

CREATE TABLE IF NOT EXISTS debt_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    customer_id     BIGINT NOT NULL COMMENT '客户ID',
    order_id        BIGINT COMMENT '关联订单ID',
    order_no        VARCHAR(30) COMMENT '关联单据编号',
    amount          DECIMAL(18,2) NOT NULL COMMENT '欠款金额',
    type            VARCHAR(20) DEFAULT 'NEW' COMMENT '类型: NEW-新增欠款/REPAID-还款',
    remark          VARCHAR(500) COMMENT '备注',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (customer_id) REFERENCES customer(id),
    INDEX idx_customer (customer_id),
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='欠款记录表';
