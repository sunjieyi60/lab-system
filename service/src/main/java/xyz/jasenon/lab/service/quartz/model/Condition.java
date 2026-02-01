package xyz.jasenon.lab.service.quartz.model;

import com.baomidou.mybatisplus.annotation.TableField;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
@TableName("`condition`")
public class Condition {
    /**
     * 条件ID
     */
    private String id;

    /**
     * 条件表达式 "#{data.id}.{properties} >=< value"
     */
    private String expr;

    /**
     * 条件描述
     */
    @TableField(value = "`desc`")
    private String desc;

    /**
     * 条件组ID
     */
    private String conditionGroupId;

    /**
     * 任务ID（便于批量查询后内存分组，减少N+1）
     */
    private String scheduleTaskId;
}
