-- H2 Test Schema for ClassTimeTable RSocket Server

-- 班牌设备表
CREATE TABLE IF NOT EXISTS class_time_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(64) NOT NULL UNIQUE,
    laboratory_id BIGINT,
    status VARCHAR(32) DEFAULT 'OFFLINE',
    config JSON,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_device_uuid ON class_time_table(uuid);
CREATE INDEX IF NOT EXISTS idx_device_laboratory ON class_time_table(laboratory_id);
CREATE INDEX IF NOT EXISTS idx_device_status ON class_time_table(status);
