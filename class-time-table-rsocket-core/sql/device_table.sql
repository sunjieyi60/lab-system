-- ============================================
-- 智慧班牌设备表（用于 RSocket 模块）
-- 简化设计，参考 model.ClassTimeTable
-- ============================================

USE `lab_sys4`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 班牌设备表
CREATE TABLE IF NOT EXISTS `class_time_table_device` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键id',
    `uuid` VARCHAR(64) NOT NULL COMMENT '班牌唯一编号',
    `config` JSON NULL COMMENT '班牌的配置信息（JSON格式）',
    `laboratory_id` BIGINT NULL COMMENT '关联的实验室id',
    `status` VARCHAR(16) DEFAULT 'offline' COMMENT '在线状态',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    
    -- 索引
    UNIQUE INDEX `uk_uuid` (`uuid`),
    INDEX `idx_laboratory_id` (`laboratory_id`),
    INDEX `idx_status` (`status`)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智慧班牌设备表';

-- 外键约束（关联实验室表）
ALTER TABLE `class_time_table_device`
    ADD CONSTRAINT `fk_device_laboratory`
        FOREIGN KEY (`laboratory_id`) REFERENCES `laboratory`(`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;

-- 初始化数据
INSERT IGNORE INTO `class_time_table_device` (
    `id`, `uuid`, `config`, `laboratory_id`, `status`
) VALUES (
    1, 
    'CTT001',
    '{"password":"123456","facePrecision":0.85,"timeout":30,"unit":"SECONDS"}',
    200,
    'offline'
);

SET FOREIGN_KEY_CHECKS = 1;
