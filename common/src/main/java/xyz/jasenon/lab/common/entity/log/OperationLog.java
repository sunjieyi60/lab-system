package xyz.jasenon.lab.common.entity.log;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 操作日志实体，对应表 operation_log。
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作人 ID。
     */
    private Long operatorId;

    /**
     * 操作人账号。
     */
    private String operatorAccount;

    /**
     * 操作人姓名。
     */
    private String operatorName;

    /**
     * 操作人角色（如 管理员/普通用户）。
     */
    private String operatorRole;

    /**
     * 操作日志类型：设备控制 / 报警联动设置 / 账号管理 等。
     */
    private String logType;

    /**
     * 操作方式：手动 / 自动 等。
     */
    private String operateWay;

    /**
     * 教室，如 16-207。
     */
    private String room;

    /**
     * 设备名称，如 空调内机1。
     */
    private String device;

    /**
     * 操作内容或报警联动设置内容。
     */
    private String content;

    /**
     * 操作发生时间。
     */
    private LocalDateTime operateTime;

    /**
     * 记录创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

