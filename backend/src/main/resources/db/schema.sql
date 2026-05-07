-- 基金基础信息表
CREATE TABLE IF NOT EXISTS fund_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    fund_code VARCHAR(10) NOT NULL UNIQUE COMMENT '基金代码',
    fund_name VARCHAR(50) NOT NULL COMMENT '基金名称',
    fund_type TINYINT COMMENT '1-宽基指数 2-行业指数 3-QDII',
    pe_current DECIMAL(10,4) COMMENT '当前PE',
    pe_percentile DECIMAL(5,2) COMMENT 'PE历史分位（0-100）',
    pb_current DECIMAL(10,4) COMMENT '当前PB',
    pb_percentile DECIMAL(5,2) COMMENT 'PB历史分位（0-100）',
    temperature INT COMMENT '估值温度（0-100）',
    daily_change DECIMAL(5,2) COMMENT '当日涨跌幅%',
    net_value DECIMAL(10,4) COMMENT '最新单位净值',
    net_value_date DATE COMMENT '净值日期',
    update_time DATETIME COMMENT '数据更新时间',
    INDEX idx_fund_code (fund_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金基础信息表';

-- 用户持仓表
CREATE TABLE IF NOT EXISTS user_fund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    total_capital DECIMAL(15,2) COMMENT '用户设定的该基金总预算',
    current_holding DECIMAL(15,2) COMMENT '当前持仓市值',
    current_cost DECIMAL(15,2) COMMENT '当前持仓成本',
    highest_price DECIMAL(10,4) COMMENT '持仓期间最高净值（移动止盈用）',
    base_monthly_amount DECIMAL(10,2) COMMENT '月基础定投金额',
    last_sell_date DATE COMMENT '最近一次卖出日期（冷却期用）',
    sell_cooldown_days INT DEFAULT 7 COMMENT '卖出冷却天数',
    profit_tier_executed TINYINT DEFAULT 0 COMMENT '已执行的止盈档位（位掩码）',
    trailing_stop_active TINYINT DEFAULT 0 COMMENT '移动止盈是否启动',
    created_at DATETIME COMMENT '创建时间',
    UNIQUE KEY uk_user_fund (user_id, fund_code),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持仓表';

-- 投资记录表
CREATE TABLE IF NOT EXISTS invest_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    date DATE COMMENT '投资日期',
    suggested_amount DECIMAL(10,2) COMMENT '系统建议金额（负数为卖出）',
    actual_amount DECIMAL(10,2) COMMENT '用户实际投入金额',
    temperature INT COMMENT '当日温度',
    reason VARCHAR(200) COMMENT '决策理由',
    sell_strategy VARCHAR(50) COMMENT '卖出策略类型',
    status TINYINT COMMENT '0-待确认 1-已执行 2-已跳过',
    INDEX idx_user_fund (user_id, fund_code),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投资记录表';

-- 插入示例基金数据
INSERT INTO fund_info (fund_code, fund_name, fund_type, temperature, update_time) VALUES
('510300', '沪深300ETF', 1, 50, NOW()),
('510500', '中证500ETF', 1, 45, NOW()),
('159915', '创业板50ETF', 1, 60, NOW()),
('513180', '恒生指数ETF', 3, 55, NOW()),
('513100', '纳指100ETF', 3, 70, NOW());