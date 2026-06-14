PRAGMA foreign_keys = ON;

-- ============================================================
-- 表1: sys_user (系统用户表)
-- 用途: 存储登录系统的用户信息
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    -- 用户ID，主键，自增
    -- 作用: 唯一标识一个用户，其他表通过 user_id 关联
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 登录账号
    -- 作用: 用户登录时使用的用户名，全局唯一，不可重复
    -- 示例: admin、zhangsan
    account         VARCHAR(50) NOT NULL UNIQUE,

    -- 登录密码
    -- 作用: 存储 BCrypt 加密后的密码密文，不存储明文
    -- 说明: 前端传入明文密码，后端用 BCrypt 加密后比对
    password        VARCHAR(100) NOT NULL,

    -- 用户真实姓名
    -- 作用: 显示在页面上的用户名称
    -- 示例: 张三、李四
    real_name       VARCHAR(50),

    -- 地址
    -- 作用: 用户的联系地址
    address         VARCHAR(200),

    -- 联系电话
    -- 作用: 用户的手机号码或固定电话
    phone           VARCHAR(20),

    -- 主营产品
    -- 作用: 用户主营产品，用于单据打印展示
    core_product    VARCHAR(100),

    -- 其他信息
    -- 作用: 备用字段，存放额外的用户描述信息
    extra_info      VARCHAR(500),

    -- 用户状态
    -- 作用: 控制用户账号是否可用
    -- 取值: 1 = 正常可用, 0 = 禁用
    status          INTEGER DEFAULT 1,

    -- 创建时间
    -- 作用: 记录用户注册的时间，自动填充
    create_time     DATETIME DEFAULT (datetime('now', '+8 hours')),

    -- 更新时间
    -- 作用: 记录用户信息最后修改的时间，自动填充
    update_time     DATETIME DEFAULT (datetime('now', '+8 hours'))
);
CREATE INDEX IF NOT EXISTS idx_account ON sys_user(account);
CREATE INDEX IF NOT EXISTS idx_phone ON sys_user(phone);


-- ============================================================
-- 表2: customer (客户表)
-- 用途: 存储客户档案信息，所有用户共享同一套客户数据
-- 说明: 客户数据是全局共享的，不同用户通过 user_customer 关联表建立关系
--       total_debt 字段已不再使用，各用户的客户欠款存于 user_customer 表中
-- ============================================================
CREATE TABLE IF NOT EXISTS customer (
    -- 客户ID，主键，自增
    -- 作用: 唯一标识一个客户
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 客户名称
    -- 作用: 客户的完整名称，如公司名称或个人姓名，必填
    -- 说明: 在系统中作为客户的唯一标识，不同用户关联同一客户时不重复创建
    customer_name   VARCHAR(100) NOT NULL,

    -- 客户分类
    -- 作用: 对客户进行归类，便于筛选管理
    -- 取值: 零售 / 批发 / 老客户，默认 "零售"
    category        VARCHAR(20) DEFAULT '零售',

    -- 联系人姓名
    -- 作用: 该客户的主要对接人姓名
    contact_name    VARCHAR(50),

    -- 联系电话
    -- 作用: 客户的联系电话，可选
    phone           VARCHAR(20),

    -- 地址
    -- 作用: 客户的经营地址或收货地址
    address         VARCHAR(200),

    -- 主营产品
    -- 作用: 客户主要经营的产品类型
    core_product    VARCHAR(100),

    -- 备注
    -- 作用: 对该客户的额外说明信息
    remark          VARCHAR(500),

    -- 累计欠款(全局)
    -- 作用: 该客户在所有用户视角下的总欠款，已废弃不再使用
    -- 说明: 由于引入了用户-客户多对多关系，实际欠款金额存储在 user_customer.debt_amount 中
    total_debt      DECIMAL(18,2) DEFAULT 0.00,

    -- 客户状态
    -- 作用: 控制客户是否有效
    -- 取值: 1 = 正常, 0 = 停用
    status          INTEGER DEFAULT 1,

    -- 创建时间
    -- 作用: 记录客户档案创建的时间
    create_time     DATETIME DEFAULT (datetime('now', '+8 hours')),

    -- 更新时间
    -- 作用: 记录客户信息最后修改的时间
    update_time     DATETIME DEFAULT (datetime('now', '+8 hours'))
);
CREATE INDEX IF NOT EXISTS idx_name ON customer(customer_name);
CREATE INDEX IF NOT EXISTS idx_phone ON customer(phone);
CREATE INDEX IF NOT EXISTS idx_category ON customer(category);


-- ============================================================
-- 表3: user_customer (用户-客户关联表)
-- 用途: 实现用户与客户之间的多对多关系，并记录每个用户视角下该客户的欠款金额
-- 说明: 一个客户可以被多个用户关联，不同用户对同一客户的欠款金额独立计算
-- ============================================================
CREATE TABLE IF NOT EXISTS user_customer (
    -- 关联记录ID，主键，自增
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 用户ID
    -- 作用: 关联 sys_user 表，标识哪个用户
    -- 外键: sys_user(id)，删除用户时级联删除关联
    user_id         INTEGER NOT NULL,

    -- 客户ID
    -- 作用: 关联 customer 表，标识哪个客户
    -- 外键: customer(id)，删除客户时级联删除关联
    customer_id     INTEGER NOT NULL,

    -- 欠款金额
    -- 作用: 记录该用户视角下此客户当前的累计欠款金额
    -- 说明: 每个用户看到的同一客户的欠款可以不同，实现数据隔离
    -- 计算方式: 该用户与该客户之间所有有效欠款记录的总和
    debt_amount     DECIMAL(18,2) DEFAULT 0.00,

    -- 创建时间
    -- 作用: 记录关联关系建立的时间
    create_time     DATETIME DEFAULT (datetime('now', '+8 hours')),

    -- 唯一约束
    -- 作用: 防止同一用户重复关联同一客户
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    UNIQUE(user_id, customer_id)
);
CREATE INDEX IF NOT EXISTS idx_uc_user ON user_customer(user_id);
CREATE INDEX IF NOT EXISTS idx_uc_customer ON user_customer(customer_id);


-- ============================================================
-- 表4: sale_order (销售订单表)
-- 用途: 存储销售订单的主表信息
-- 说明: 每笔交易对应一条订单记录，订单金额由商品明细自动汇总
-- ============================================================
CREATE TABLE IF NOT EXISTS sale_order (
    -- 订单ID，主键，自增
    -- 作用: 唯一标识一笔订单
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 单据编号
    -- 作用: 订单的唯一业务编号，用于打印单据和人工识别
    -- 格式: XSDD + 年月日时分秒毫秒，如 XSDD20260529143025123
    -- 约束: 全局唯一，不可重复
    order_no            VARCHAR(30) NOT NULL UNIQUE,

    -- 用户ID
    -- 作用: 关联 sys_user 表，标识该订单由哪个用户创建
    -- 说明: 实现订单数据隔离，每个用户只能看到自己创建的订单
    user_id             INTEGER NOT NULL,

    -- 客户ID
    -- 作用: 关联 customer 表，标识该订单属于哪个客户
    -- 说明: 只能关联当前用户已建立关系的客户
    customer_id         INTEGER NOT NULL,

    -- 销售人员
    -- 作用: 记录负责该订单的销售人员姓名
    -- 默认值: 当前登录用户的姓名
    sales_person        VARCHAR(50),

    -- 单据日期
    -- 作用: 订单的签订日期，用于按日期筛选和统计
    -- 格式: YYYY-MM-DD
    order_date          DATE,

    -- 交货日期
    -- 作用: 约定的商品交付日期
    -- 格式: YYYY-MM-DD
    delivery_date       DATE,

    -- 订单状态
    -- 作用: 标识订单当前所处阶段
    -- 取值: 进行中(默认) / 已完成 / 已作废
    status              VARCHAR(20) DEFAULT '进行中',

    -- 本单金额
    -- 作用: 订单中所有商品明细的金额合计
    -- 计算方式: SUM(sale_order_item.amount)
    total_amount        DECIMAL(18,2) DEFAULT 0.00,

    -- 其他费用
    -- 作用: 订单中除商品金额外的额外费用，如运费、包装费等
    -- 说明: 可为正数或0，不能为负数
    other_fee           DECIMAL(18,2) DEFAULT 0.00,

    -- 应收金额
    -- 作用: 本次交易应收取的总金额
    -- 计算方式: total_amount + other_fee
    receivable_amount   DECIMAL(18,2) DEFAULT 0.00,

    -- 本次收款
    -- 作用: 客户本次实际支付的金额
    -- 说明: 若为赊账方式，此值通常为0或部分金额
    received_amount     DECIMAL(18,2) DEFAULT 0.00,

    -- 本次欠款
    -- 作用: 本次交易中客户尚未支付的金额
    -- 计算方式: receivable_amount - received_amount
    -- 说明: 大于0时会在 debt_record 中生成欠款记录
    current_debt        DECIMAL(18,2) DEFAULT 0.00,

    -- 累计欠款
    -- 作用: 该客户的历史累计欠款，冗余字段
    total_debt          DECIMAL(18,2) DEFAULT 0.00,

    -- 收款方式
    -- 作用: 标识客户支付的方式
    -- 取值: 现金 / 微信 / 支付宝 / 银行转账 / 赊账
    payment_method      VARCHAR(30),

    -- 备注
    -- 作用: 对整张订单的额外说明信息
    remark              VARCHAR(1000),

    -- 创建时间
    -- 作用: 记录订单创建的时间
    create_time         DATETIME DEFAULT (datetime('now', '+8 hours')),

    -- 更新时间
    -- 作用: 记录订单最后修改的时间
    update_time         DATETIME DEFAULT (datetime('now', '+8 hours'))
);
CREATE INDEX IF NOT EXISTS idx_order_no ON sale_order(order_no);
CREATE INDEX IF NOT EXISTS idx_user_id ON sale_order(user_id);
CREATE INDEX IF NOT EXISTS idx_customer ON sale_order(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_date ON sale_order(order_date);
CREATE INDEX IF NOT EXISTS idx_status ON sale_order(status);


-- ============================================================
-- 表5: sale_order_item (订单明细表)
-- 用途: 存储订单中每个商品的详细信息
-- 说明: 一条订单可包含多条商品明细，订单总金额由明细汇总
-- ============================================================
CREATE TABLE IF NOT EXISTS sale_order_item (
    -- 明细ID，主键，自增
    -- 作用: 唯一标识一条订单明细
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 订单ID
    -- 作用: 关联 sale_order 表，标识该明细属于哪个订单
    -- 外键: sale_order(id)，删除订单时级联删除明细
    order_id        INTEGER NOT NULL,

    -- 序号
    -- 作用: 商品在订单中的显示顺序，从1开始递增
    -- 说明: 用于保持商品明细的排列顺序
    seq_no          INTEGER DEFAULT 1,

    -- 商品编号
    -- 作用: 若从商品库中选择，则填充对应商品的编码
    -- 示例: BZZ-42-19(白针织42# 1.9mm)
    -- 说明: 手工填写的商品此字段可为空
    product_code    VARCHAR(50),

    -- 商品名称
    -- 作用: 商品的完整名称，显示在订单和打印单据上，必填
    -- 示例: 白针织42#、黑针织37#
    product_name    VARCHAR(200) NOT NULL,

    -- 规格型号
    -- 作用: 商品的具体规格描述
    -- 示例: 1.9mm/6分、2.2mm/7分
    specification   VARCHAR(100),

    -- 计量单位
    -- 作用: 商品的计量单位
    -- 示例: 公斤、米、件，默认 "公斤"
    unit            VARCHAR(20),

    -- 数量
    -- 作用: 该商品的销售数量
    -- 说明: 支持小数，最多3位小数
    quantity        DECIMAL(18,3) DEFAULT 0.000,

    -- 单价
    -- 作用: 该商品的单个价格(元)
    -- 说明: 支持小数，不能为负数
    unit_price      DECIMAL(18,2) DEFAULT 0.00,

    -- 金额
    -- 作用: 该商品的总金额
    -- 计算方式: quantity * unit_price
    amount          DECIMAL(18,2) DEFAULT 0.00,

    -- 备注
    -- 作用: 对该商品明细的额外说明
    remark          VARCHAR(200),

    -- 创建时间
    -- 作用: 记录该明细添加的时间
    create_time     DATETIME DEFAULT (datetime('now', '+8 hours')),

    FOREIGN KEY (order_id) REFERENCES sale_order(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_order_id ON sale_order_item(order_id);


-- ============================================================
-- 表6: debt_record (欠款记录表)
-- 用途: 记录每一笔订单产生的欠款明细，用于追踪欠款状态
-- 说明:
--   - 通过 user_id 实现数据隔离，每个用户只能看到自己的欠款记录
--   - 通过 order_id 关联到具体订单
--   - status = UNSETTLED 表示该欠款尚未开始还款
--   - status = PARTIAL  表示已部分还款（0 < settled_amount < amount）
--   - status = SETTLED  表示已全额还清（settled_amount = amount）
--   - 还款通过 debt_repayment 关联表实现核销，不直接修改 debt_record 的 amount
--   - 每笔欠款独立存在，修改订单时：新增欠款则新建记录，减少欠款则通过还款核销
-- ============================================================
CREATE TABLE IF NOT EXISTS debt_record (
    -- 记录ID，主键，自增
    -- 作用: 唯一标识一条欠款记录
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 用户ID
    -- 作用: 关联 sys_user 表，标识该欠款记录属于哪个用户
    -- 说明: 实现欠款数据隔离，A用户看不到B用户的欠款记录
    user_id         INTEGER NOT NULL,

    -- 客户ID
    -- 作用: 关联 customer 表，标识该欠款属于哪个客户
    customer_id     INTEGER NOT NULL,

    -- 订单ID
    -- 作用: 关联 sale_order 表，标识该欠款由哪个订单产生
    -- 说明: 通过 order_id 可以找到该订单对应的欠款记录
    order_id        INTEGER,

    -- 单据编号
    -- 作用: 冗余存储订单的单据编号，便于直接查看
    order_no        VARCHAR(30),

    -- 欠款金额
    -- 作用: 该笔记录的原始欠款金额，始终为正数
    -- 说明: 记录创建时的欠款金额，后续不再变化
    amount          DECIMAL(18,2) NOT NULL,

    -- 已还金额
    -- 作用: 该笔欠款已经被还款的金额
    -- 说明:
    --   初始为 0，每次还款核销后累加
    --   settled_amount = amount 时表示已全额还清
    settled_amount  DECIMAL(18,2) DEFAULT 0.00,

    -- 欠款状态
    -- 作用: 标识该欠款记录的结清状态
    -- 取值: UNSETTLED = 未结清(默认), PARTIAL = 部分结清, SETTLED = 已结清
    -- 说明: 用于筛选未结清的欠款记录
    status          VARCHAR(20) DEFAULT 'UNSETTLED',

    -- 备注
    -- 作用: 对该笔欠款记录的说明，如产生原因
    -- 示例: "订单欠款，收款方式：赊账"、"订单修改，新增欠款"
    remark          VARCHAR(500),

    -- 创建时间
    -- 作用: 记录该欠款记录产生的时间
    create_time     DATETIME DEFAULT (datetime('now', '+8 hours')),

    FOREIGN KEY (customer_id) REFERENCES customer(id)
);
CREATE INDEX IF NOT EXISTS idx_debt_user ON debt_record(user_id);
CREATE INDEX IF NOT EXISTS idx_debt_customer ON debt_record(customer_id);
CREATE INDEX IF NOT EXISTS idx_debt_order ON debt_record(order_id);
CREATE INDEX IF NOT EXISTS idx_debt_status ON debt_record(status);


-- ============================================================
-- 表6: repayment_record (还款记录表)
-- 用途: 记录客户的每一笔独立还款，与订单解耦
-- 说明:
--   - 还款是独立的业务事件，客户可以随时还款，不一定跟订单修改挂钩
--   - 一笔还款可以核销多笔欠款（通过 debt_repayment 关联表）
--   - 支持现金/微信/支付宝/银行转账等多种还款方式
-- ============================================================
CREATE TABLE IF NOT EXISTS repayment_record (
    -- 记录ID，主键，自增
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 用户ID
    -- 作用: 关联 sys_user 表，标识该还款记录属于哪个用户
    user_id         INTEGER NOT NULL,

    -- 客户ID
    -- 作用: 关联 customer 表，标识该还款属于哪个客户
    customer_id     INTEGER NOT NULL,

    -- 还款金额
    -- 作用: 客户本次实际还款的金额
    -- 说明: 始终为正数，还款总额应小于等于该客户的未结清欠款总额
    amount          DECIMAL(18,2) NOT NULL,

    -- 还款方式
    -- 作用: 标识客户还款时使用的方式
    -- 取值: 现金 / 微信 / 支付宝 / 银行转账
    payment_method  VARCHAR(30),

    -- 还款日期
    -- 作用: 客户实际还款的日期
    repayment_date  DATE,

    -- 备注
    -- 作用: 对该笔还款的额外说明
    -- 示例: "客户到店现金还款"、"订单作废退款"
    remark          VARCHAR(500),

    -- 创建时间
    -- 作用: 记录该还款记录产生的时间
    create_time     DATETIME DEFAULT (datetime('now', '+8 hours')),

    FOREIGN KEY (customer_id) REFERENCES customer(id)
);
CREATE INDEX IF NOT EXISTS idx_repay_user ON repayment_record(user_id);
CREATE INDEX IF NOT EXISTS idx_repay_customer ON repayment_record(customer_id);
CREATE INDEX IF NOT EXISTS idx_repay_date ON repayment_record(repayment_date);


-- ============================================================
-- 表7: debt_repayment (欠款-还款核销关联表)
-- 用途: 记录每笔还款具体核销了哪几笔欠款的多少金额
-- 说明:
--   - 这是一个多对多关联表：一笔还款可以核销多笔欠款，一笔欠款可以被多笔还款核销
--   - 通过此表可以精确追溯：某笔还款还了哪些订单的欠款、各还了多少
--   - 每笔核销记录的 amount 表示本次核销的金额
-- ============================================================
CREATE TABLE IF NOT EXISTS debt_repayment (
    -- 记录ID，主键，自增
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 欠款记录ID
    -- 作用: 关联 debt_record 表，标识核销了哪笔欠款
    debt_id         INTEGER NOT NULL,

    -- 还款记录ID
    -- 作用: 关联 repayment_record 表，标识由哪笔还款核销
    repayment_id    INTEGER NOT NULL,

    -- 核销金额
    -- 作用: 本次核销的具体金额
    -- 说明: 单笔核销金额不能超过对应欠款的剩余未还金额
    amount          DECIMAL(18,2) NOT NULL,

    -- 创建时间
    -- 作用: 记录核销操作的时间
    create_time     DATETIME DEFAULT (datetime('now', '+8 hours')),

    FOREIGN KEY (debt_id) REFERENCES debt_record(id),
    FOREIGN KEY (repayment_id) REFERENCES repayment_record(id)
);
CREATE INDEX IF NOT EXISTS idx_dr_debt ON debt_repayment(debt_id);
CREATE INDEX IF NOT EXISTS idx_dr_repayment ON debt_repayment(repayment_id);


-- ============================================================
-- 表7: product (商品表)
-- 用途: 存储商品基础信息，作为订单明细中商品选择的数据来源
-- 说明: 订单中可以手工填写商品，也可以从本表选择商品自动填充信息
--       商品按 product_name(如"白针织42#")分组，每组包含多个规格
-- ============================================================
CREATE TABLE IF NOT EXISTS product (
    -- 商品ID，主键，自增
    id              INTEGER PRIMARY KEY AUTOINCREMENT,

    -- 商品编码，全局唯一
    -- 格式: {颜色首字母}{品类}{型号}-{规格数字}
    -- 示例: BZZ-42-19 = 白(B)针织(ZZ)42# 1.9cm/6分
    product_code    VARCHAR(50) NOT NULL UNIQUE,

    -- 商品名称，用于树形选择一级分组
    -- 示例: 针织42#、针织37#、针织32#
    product_name    VARCHAR(200) NOT NULL,

    -- 规格型号，格式: 颜色;尺码;备注
    -- 示例: 白;1.9cm/6分;码数45y
    specification   VARCHAR(200),

    -- 计量单位，默认公斤
    unit            VARCHAR(20) DEFAULT '公斤',

    -- 标准单价(元/单位)
    unit_price      DECIMAL(18,2) DEFAULT 0.00,

    -- 备注
    remark          VARCHAR(200),

    -- 所属用户ID，实现用户数据隔离
    user_id         INTEGER DEFAULT 1,

    -- 创建时间
    create_time     DATETIME DEFAULT (datetime('now', '+8 hours')),

    -- 更新时间
    update_time     DATETIME DEFAULT (datetime('now', '+8 hours'))
);
CREATE INDEX IF NOT EXISTS idx_product_code ON product(product_code);
CREATE INDEX IF NOT EXISTS idx_product_name ON product(product_name);
CREATE INDEX IF NOT EXISTS idx_product_user_id ON product(user_id);

-- 为已有数据库添加 user_id 列（SQLite 不支持 IF NOT EXISTS for ALTER，忽略错误）
-- ALTER TABLE product ADD COLUMN user_id INTEGER DEFAULT 1;
