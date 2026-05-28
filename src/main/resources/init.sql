PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS sys_user (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    account         VARCHAR(50) NOT NULL UNIQUE,
    password        VARCHAR(100) NOT NULL,
    real_name       VARCHAR(50),
    address         VARCHAR(200),
    phone           VARCHAR(20),
    extra_info      VARCHAR(500),
    status          INTEGER DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_account ON sys_user(account);
CREATE INDEX IF NOT EXISTS idx_phone ON sys_user(phone);

CREATE TABLE IF NOT EXISTS customer (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_name   VARCHAR(100) NOT NULL,
    category        VARCHAR(20) DEFAULT '零售',
    contact_name    VARCHAR(50),
    phone           VARCHAR(20),
    address         VARCHAR(200),
    remark          VARCHAR(500),
    total_debt      DECIMAL(18,2) DEFAULT 0.00,
    status          INTEGER DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_name ON customer(customer_name);
CREATE INDEX IF NOT EXISTS idx_phone ON customer(phone);
CREATE INDEX IF NOT EXISTS idx_category ON customer(category);

CREATE TABLE IF NOT EXISTS sale_order (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no            VARCHAR(30) NOT NULL UNIQUE,
    customer_id         INTEGER NOT NULL,
    sales_person        VARCHAR(50),
    order_date          DATE,
    delivery_date       DATE,
    status              VARCHAR(20) DEFAULT '进行中',
    total_amount        DECIMAL(18,2) DEFAULT 0.00,
    other_fee           DECIMAL(18,2) DEFAULT 0.00,
    receivable_amount   DECIMAL(18,2) DEFAULT 0.00,
    received_amount     DECIMAL(18,2) DEFAULT 0.00,
    current_debt        DECIMAL(18,2) DEFAULT 0.00,
    total_debt          DECIMAL(18,2) DEFAULT 0.00,
    payment_method      VARCHAR(30),
    remark              VARCHAR(1000),
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_order_no ON sale_order(order_no);
CREATE INDEX IF NOT EXISTS idx_customer ON sale_order(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_date ON sale_order(order_date);
CREATE INDEX IF NOT EXISTS idx_status ON sale_order(status);

CREATE TABLE IF NOT EXISTS sale_order_item (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id        INTEGER NOT NULL,
    seq_no          INTEGER DEFAULT 1,
    product_code    VARCHAR(50),
    product_name    VARCHAR(200) NOT NULL,
    specification   VARCHAR(100),
    unit            VARCHAR(20),
    quantity        DECIMAL(18,3) DEFAULT 0.000,
    unit_price      DECIMAL(18,2) DEFAULT 0.00,
    amount          DECIMAL(18,2) DEFAULT 0.00,
    remark          VARCHAR(200),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES sale_order(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_order_id ON sale_order_item(order_id);

CREATE TABLE IF NOT EXISTS debt_record (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id     INTEGER NOT NULL,
    order_id        INTEGER,
    order_no        VARCHAR(30),
    amount          DECIMAL(18,2) NOT NULL,
    type            VARCHAR(20) DEFAULT 'NEW',
    remark          VARCHAR(500),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);
CREATE INDEX IF NOT EXISTS idx_debt_customer ON debt_record(customer_id);
CREATE INDEX IF NOT EXISTS idx_debt_order ON debt_record(order_id);
