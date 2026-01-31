package xyz.jasenon.lab.common.entity.log;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 报警日志实体，对应表 alarm_log。
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("alarm_log")
public class AlarmLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 教室，如 207。
     */
    private String room;

    /**
     * 设备名称；条件触发报警时可以为空。
     */
    private String device;

    /**
     * 报警时间。
     */
    private LocalDateTime alarmTime;

    /**
     * 报警大类：设备异常 / 条件触发。
     */
    private String category;

    /**
     * 报警类型：门禁报警 / 电气报警 / 中央空调报警 / 环境报警 等。
     */
    private String alarmType;

    /**
     * 报警内容。
     */
    private String content;

    /**
     * 记录创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 网关 ID（仅用于接收：room 为空时按此补全教室，不落库）。
     */
    @TableField(exist = false)
    private Long gatewayId;
}

