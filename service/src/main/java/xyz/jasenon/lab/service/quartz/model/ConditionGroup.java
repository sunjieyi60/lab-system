package xyz.jasenon.lab.service.quartz.model;

import com.baomidou.mybatisplus.annotation.TableField;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
public class ConditionGroup {

    /**
     * 条件组ID
     */
    private String id;

    /**
     * 条件组类型
     */
    private ConditionGroupType type;

    /**
     * 任务ID
     */
    private String scheduleTaskId;

    /**
     * 条件列表
     */
    @TableField(exist = false)
    private List<Condition> conditions;

    public enum ConditionGroupType {
        ALL,
        ANY;
    }

}
