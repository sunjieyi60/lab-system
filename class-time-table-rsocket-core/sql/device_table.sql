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

-- 人脸图片库表 - 用于云端管理班牌人脸图片
CREATE TABLE IF NOT EXISTS `face_image_library` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键id',
    `uuid` VARCHAR(64) NOT NULL COMMENT '班牌UUID',
    `face_feature_name` VARCHAR(128) NOT NULL COMMENT '人脸特征名称',
    `image_urls` JSON NULL COMMENT '图片URL列表，用于web渲染人脸库',
    `face_feature` BLOB NULL COMMENT '人脸特征数据（由client提取后回传）',
    `image_count` INT DEFAULT 0 COMMENT '图片数量',
    `status` VARCHAR(32) DEFAULT 'UPLOADING' COMMENT '状态: UPLOADING/COMPLETED/FAILED',
    `task_id` VARCHAR(64) NULL COMMENT '关联的上传任务ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    
    -- 索引
    INDEX `idx_face_img_uuid` (`uuid`),
    INDEX `idx_face_img_task_id` (`task_id`),
    INDEX `idx_face_img_status` (`status`)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸图片库';

-- 外键约束（关联实验室表）
ALTER TABLE `class_time_table_device`
    ADD CONSTRAINT `fk_device_laboratory`
        FOREIGN KEY (`laboratory_id`) REFERENCES `laboratory`(`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;
