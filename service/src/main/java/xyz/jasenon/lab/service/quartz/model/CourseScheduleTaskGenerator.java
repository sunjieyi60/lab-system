package xyz.jasenon.lab.service.quartz.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CourseScheduleTaskGenerator {
    /**
     * 实验室ids
     */
    @NotEmpty(message = "实验室id不能为空")
    private List<Long> laboratoryId;
    /**
     * 检查间隔
     */
    private String cron = "0 0/5 * * * ? ";
    /**
     * 提前执行的时间  默认7分钟
     */
    private Integer earlyStart = 7;
    /**
     * 延迟结束的时间  默认7分钟
     */
    private Integer delayEnd = 7;
    /**
     * 是否启用
     */
    private Boolean enable = true;
    /**
     * 学期id
     */
    @NotNull
    private Long semesterId;
    /**
     * 条件组模板（可选）
     * 每个生成的任务都会深度复制一份，并重新分配ID
     */
    private List<ConditionGroup> conditionGroups;
    /**
     * 动作组模板（可选）
     * 每个生成的任务都会深度复制一份，并重新分配ID
     * conditionGroupId 会按照原始关联自动映射到新条件组ID
     */
    private List<ActionGroup> actionGroups;
    /**
     * 数据源模板（可选）
     * 每个生成的任务都会深度复制一份，并重新分配ID
     */
    private List<Data> dataGroup;

}
