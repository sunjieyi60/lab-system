package xyz.jasenon.lab.service.quartz.model;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
public class ActionGroup {

    /**
     * 动作组ID
     */
    private String id;

    /**
     * 调度任务ID
     */
    private String scheduleTaskId;

    /**
     * 条件组ID
     */
    private String conditionGroupId;

    /**
     * 动作列表
     */
    @TableField(exist = false)
    private List<Action> actions;
}
