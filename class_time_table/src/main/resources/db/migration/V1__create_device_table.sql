-- 创建设备表
CREATE TABLE IF NOT EXISTS `smart_board_device` (
    `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID（唯一标识）',
    `device_name` VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
    `mac_address` VARCHAR(32) DEFAULT NULL COMMENT '设备MAC地址',
    `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '设备IP地址',
    `device_type` VARCHAR(32) DEFAULT NULL COMMENT '设备类型（如：smartboard, gateway等）',
    `hardware_info` TEXT DEFAULT NULL COMMENT '硬件信息（JSON字符串）',
    `status` VARCHAR(16) DEFAULT 'OFFLINE' COMMENT '设备状态：ONLINE, OFFLINE',
    `version` VARCHAR(32) DEFAULT NULL COMMENT '设备版本号',
    `last_online_time` DATETIME DEFAULT NULL COMMENT '最后在线时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `config_json` TEXT DEFAULT NULL COMMENT '设备配置JSON（最新配置）',
    PRIMARY KEY (`device_id`),
    INDEX `idx_mac` (`mac_address`),
    INDEX `idx_status` (`status`),
    INDEX `idx_last_online` (`last_online_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能班牌设备表';

