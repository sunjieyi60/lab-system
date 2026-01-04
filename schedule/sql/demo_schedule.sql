-- Demo schema for scheduler (MySQL)
CREATE DATABASE IF NOT EXISTS schedule_demo DEFAULT CHARACTER SET utf8mb4;
USE schedule_demo;

CREATE TABLE IF NOT EXISTS task_group (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    cron_expr VARCHAR(64) NOT NULL,
    enable TINYINT DEFAULT 1,
    misfire_policy VARCHAR(32) DEFAULT 'SMART_POLICY',
    desc_text VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS time_rule (
    id BIGINT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    business_time VARCHAR(255),   -- e.g. "09:00-12:00,13:30-18:00"
    weekdays VARCHAR(64),         -- e.g. "1,2,3,4,5"
    week_parity VARCHAR(8),       -- ALL/ODD/EVEN
    date_range VARCHAR(64),       -- e.g. "2025-01-01~2025-12-31"
    timezone VARCHAR(64) DEFAULT 'Asia/Shanghai'
);

CREATE TABLE IF NOT EXISTS data_group (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128),
    type VARCHAR(32),             -- TEMPERATURE / LIGHT / POWER / ...
    fetch_config JSON,
    agg VARCHAR(16) DEFAULT 'AVG',
    mock_value DOUBLE,
    enable TINYINT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS condition_group (
    id BIGINT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    logic VARCHAR(8) DEFAULT 'ALL',   -- ALL / ANY
    enable TINYINT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS cond (
    id BIGINT PRIMARY KEY,
    condition_group_id BIGINT NOT NULL,
    expr VARCHAR(255) NOT NULL,       -- SpEL / MVEL
    data_group_id BIGINT,
    desc_text VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS action (
    id BIGINT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    name VARCHAR(128),
    type VARCHAR(32),                 -- HTTP / SQL / MQ / SMS / SMTP / CUSTOM
    payload JSON,
    order_no INT DEFAULT 1,
    parallel_tag VARCHAR(32),
    retry_times INT DEFAULT 0,
    retry_backoff_ms INT DEFAULT 0,
    timeout_ms INT DEFAULT 5000,
    enable TINYINT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS alert_channel (
    id BIGINT PRIMARY KEY,
    type VARCHAR(16),                 -- SMS / SMTP
    config JSON,
    enable TINYINT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS task_group_alert (
    id BIGINT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    enable TINYINT DEFAULT 1,
    throttle_ms BIGINT DEFAULT 600000,
    continuous_fail_threshold INT DEFAULT 1,
    bind_channels VARCHAR(128)        -- comma separated channel ids
);

-- Mock data
INSERT INTO task_group (id, name, cron_expr, enable, misfire_policy, desc_text) VALUES
(1, '环境监控-告警任务组', '0 */5 * * * ?', 1, 'SMART_POLICY', '温度/光照异常告警')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO time_rule (id, group_id, business_time, weekdays, week_parity, date_range, timezone) VALUES
(1, 1, '09:00-12:00,13:30-18:00', '1,2,3,4,5', 'ALL', '2025-01-01~2025-12-31', 'Asia/Shanghai')
ON DUPLICATE KEY UPDATE business_time=VALUES(business_time);

INSERT INTO data_group (id, name, type, fetch_config, agg, mock_value, enable) VALUES
(100, '室内温度', 'TEMPERATURE', JSON_OBJECT('deviceId','temp-001','metric','temp','windowMinutes',5,'agg','AVG'), 'AVG', 32.5, 1),
(101, '室内光照', 'LIGHT', JSON_OBJECT('deviceId','light-001','metric','lux','windowMinutes',5,'agg','AVG'), 'AVG', 120, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO condition_group (id, group_id, logic, enable) VALUES
(200, 1, 'ALL', 1)
ON DUPLICATE KEY UPDATE logic=VALUES(logic);

INSERT INTO cond (id, condition_group_id, expr, data_group_id, desc_text) VALUES
(201, 200, '#data[100] > 30', 100, '温度 > 30℃'),
(202, 200, '#data[101] < 200', 101, '光照 < 200 lux')
ON DUPLICATE KEY UPDATE expr=VALUES(expr);

INSERT INTO action (id, group_id, name, type, payload, order_no, parallel_tag, retry_times, retry_backoff_ms, timeout_ms, enable) VALUES
(301, 1, '推送短信', 'SMS', JSON_OBJECT('to','13800000000','template','温度告警','vars', JSON_OBJECT('temp','#data[100]')), 1, NULL, 1, 1000, 5000, 1),
(302, 1, '发送邮件', 'SMTP', JSON_OBJECT('to','ops@example.com','subject','温度光照告警','body','温度与光照异常'), 2, NULL, 0, 0, 5000, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO alert_channel (id, type, config, enable) VALUES
(401, 'SMS', JSON_OBJECT('provider','mock','apiKey','demo'), 1),
(402, 'SMTP', JSON_OBJECT('host','smtp.example.com','from','noreply@example.com','to','ops@example.com'), 1)
ON DUPLICATE KEY UPDATE type=VALUES(type);

INSERT INTO task_group_alert (id, group_id, enable, throttle_ms, continuous_fail_threshold, bind_channels) VALUES
(501, 1, 1, 600000, 1, '401,402')
ON DUPLICATE KEY UPDATE enable=VALUES(enable);


