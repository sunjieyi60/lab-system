package xyz.jasenon.lab.service.quartz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import xyz.jasenon.lab.service.quartz.mapper.*;
import xyz.jasenon.lab.service.quartz.model.*;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConfigBatchLoader {

    private final ScheduleTaskMapper scheduleTaskMapper;
    private final ActionGroupMapper actionGroupMapper;
    private final ActionMapper actionMapper;
    private final ConditionGroupMapper conditionGroupMapper;
    private final ConditionMapper conditionMapper;
    private final DataMapper dataMapper;
    private final AlarmMapper alarmMapper;
    private final TimeRuleMapper timeRuleMapper;

    /**
     * 高性能批量加载：传入候选taskId，批量组装完整配置
     * 仅2轮查询：1轮查所有主表 + 1轮查所有子表
     */
    public List<ScheduleConfigRoot> batchLoadByTaskIds(List<String> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Collections.emptyList();
        }

        // 第1轮：批量查询所有主表数据
        List<ScheduleTask> tasks = scheduleTaskMapper.selectList(
            new LambdaQueryWrapper<ScheduleTask>()
                .in(ScheduleTask::getId, taskIds)
                .orderByDesc(ScheduleTask::getId)
        );

        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        // 第2轮：批量查询所有子表数据（使用 MyBatis-Plus Wrapper）
        List<ActionGroup> actionGroups = actionGroupMapper.selectList(
            new LambdaQueryWrapper<ActionGroup>()
                .in(ActionGroup::getScheduleTaskId, taskIds)
        );
        List<Action> allActions = actionMapper.selectList(
            new LambdaQueryWrapper<Action>()
                .in(Action::getScheduleTaskId, taskIds)
        );
        List<ConditionGroup> conditionGroups = conditionGroupMapper.selectList(
            new LambdaQueryWrapper<ConditionGroup>()
                .in(ConditionGroup::getScheduleTaskId, taskIds)
        );
        List<Condition> allConditions = conditionMapper.selectList(
            new LambdaQueryWrapper<Condition>()
                .in(Condition::getScheduleTaskId, taskIds)
        );
        List<Data> datas = dataMapper.selectList(
            new LambdaQueryWrapper<Data>()
                .in(Data::getScheduleTaskId, taskIds)
        );
        List<Alarm> alarms = alarmMapper.selectList(
            new LambdaQueryWrapper<Alarm>()
                .in(Alarm::getScheduleTaskId, taskIds)
        );
        List<TimeRule> timeRules = timeRuleMapper.selectList(
            new LambdaQueryWrapper<TimeRule>()
                .in(TimeRule::getScheduleTaskId, taskIds)
        );

        // 内存分组（用HashMap提高性能）
        Map<String, List<ActionGroup>> actionGroupMap = actionGroups.stream()
            .collect(Collectors.groupingBy(ActionGroup::getScheduleTaskId));

        Map<String, List<Action>> actionMap = allActions.stream()
            .collect(Collectors.groupingBy(Action::getActionGroupId));

        Map<String, List<ConditionGroup>> conditionGroupMap = conditionGroups.stream()
            .collect(Collectors.groupingBy(ConditionGroup::getScheduleTaskId));

        Map<String, List<Condition>> conditionMap = allConditions.stream()
            .collect(Collectors.groupingBy(Condition::getConditionGroupId));

        Map<String, List<Data>> dataMap = datas.stream()
            .collect(Collectors.groupingBy(Data::getScheduleTaskId));

        Map<String, List<Alarm>> alarmMap = alarms.stream()
            .collect(Collectors.groupingBy(Alarm::getScheduleTaskId));

        Map<String, TimeRule> timeRuleMap = timeRules.stream()
            .collect(Collectors.toMap(TimeRule::getScheduleTaskId, tr -> tr, (a, b) -> a));

        // 组装结果
        List<ScheduleConfigRoot> results = new ArrayList<>(tasks.size());
        for (ScheduleTask task : tasks) {
            ScheduleConfigRoot config = new ScheduleConfigRoot();
            config.setTask(task);

            // 组装ActionGroups
            List<ActionGroup> ags = actionGroupMap.getOrDefault(task.getId(), Collections.emptyList());
            for (ActionGroup ag : ags) {
                ag.setActions(actionMap.getOrDefault(ag.getId(), Collections.emptyList()));
            }
            config.setActionGroups(ags);

            // 组装ConditionGroups
            List<ConditionGroup> cgs = conditionGroupMap.getOrDefault(task.getId(), Collections.emptyList());
            for (ConditionGroup cg : cgs) {
                cg.setConditions(conditionMap.getOrDefault(cg.getId(), Collections.emptyList()));
            }
            config.setConditionGroups(cgs);

            // 其他直接设置
            config.setDataGroup(dataMap.getOrDefault(task.getId(), Collections.emptyList()));
            config.setAlarmGroup(alarmMap.getOrDefault(task.getId(), Collections.emptyList()));
            config.setTimeRule(timeRuleMap.get(task.getId()));

            results.add(config);
        }

        return results;
    }
}