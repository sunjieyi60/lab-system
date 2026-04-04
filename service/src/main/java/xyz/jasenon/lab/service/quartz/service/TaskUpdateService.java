package xyz.jasenon.lab.service.quartz.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.quartz.mapper.*;
import xyz.jasenon.lab.service.quartz.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskUpdateService {

    private final ScheduleTaskMapper scheduleTaskMapper;
    private final ConditionMapper conditionMapper;
    private final ConditionGroupMapper conditionGroupMapper;
    private final ActionGroupMapper actionGroupMapper;
    private final ActionMapper actionMapper;
    private final TimeRuleMapper timeRuleMapper;
    private final AlarmMapper alarmMapper;
    private final DataMapper dataMapper;
    private final ConfigLoader configLoader;

    /**
     * 更新定时任务配置
     * 采用 "select for update -> delete -> insert" 策略保证数据一致性
     *
     * @param root 完整的任务配置
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateTask(ScheduleConfigRoot root) {
        // 1. 验证配置参数合法性
        Boolean isOk = configLoader.verifyConfig(root).getData();
        if (!isOk) {
            throw R.badRequest("参数错误").convert();
        }

        ScheduleTask newTask = root.getTask();
        String taskId = newTask.getId();

        // 2. 查询并锁定任务记录（for update）
        ScheduleTask existTask = scheduleTaskMapper.selectById(taskId);
        if (existTask == null) {
            return R.fail("任务不存在: " + taskId);
        }

        // 3. 删除旧的任务关联数据（按照依赖关系的逆序删除）
        // 3.1 删除条件（condition）
        conditionMapper.delete(
                new LambdaQueryWrapper<Condition>()
                        .eq(Condition::getScheduleTaskId, taskId)
        );

        // 3.2 删除条件组（condition_group）
        conditionGroupMapper.delete(
                new LambdaQueryWrapper<ConditionGroup>()
                        .eq(ConditionGroup::getScheduleTaskId, taskId)
        );

        // 3.3 删除动作（action）
        actionMapper.delete(
                new LambdaQueryWrapper<Action>()
                        .eq(Action::getScheduleTaskId, taskId)
        );

        // 3.4 删除动作组（action_group）
        actionGroupMapper.delete(
                new LambdaQueryWrapper<ActionGroup>()
                        .eq(ActionGroup::getScheduleTaskId, taskId)
        );

        // 3.5 删除数据源（data）
        dataMapper.delete(
                new LambdaQueryWrapper<Data>()
                        .eq(Data::getScheduleTaskId, taskId)
        );

        // 3.6 删除报警配置（alarm）
        alarmMapper.delete(
                new LambdaQueryWrapper<Alarm>()
                        .eq(Alarm::getScheduleTaskId, taskId)
        );

        // 3.7 删除时间规则（time_rule）
        timeRuleMapper.delete(
                new LambdaQueryWrapper<TimeRule>()
                        .eq(TimeRule::getScheduleTaskId, taskId)
        );

        // 3.8 删除原任务主体（schedule_task）
        scheduleTaskMapper.deleteById(taskId);

        // 4. 重新插入新的配置数据
        // 4.1 插入任务主体
        scheduleTaskMapper.insert(newTask);

        // 4.2 插入条件组和条件
        List<ConditionGroup> conditionGroups = root.getConditionGroups();
        if (conditionGroups != null && !conditionGroups.isEmpty()) {
            for (ConditionGroup conditionGroup : conditionGroups) {
                // 生成新的条件组ID（如果前端未提供）
                if (conditionGroup.getId() == null) {
                    conditionGroup.setId(IdUtil.getSnowflakeNextIdStr());
                }
                conditionGroup.setScheduleTaskId(taskId);
                conditionGroupMapper.insert(conditionGroup);

                // 插入该条件组下的所有条件
                List<Condition> conditions = conditionGroup.getConditions();
                if (conditions != null && !conditions.isEmpty()) {
                    for (Condition condition : conditions) {
                        if (condition.getId() == null) {
                            condition.setId(IdUtil.getSnowflakeNextIdStr());
                        }
                        condition.setConditionGroupId(conditionGroup.getId());
                        condition.setScheduleTaskId(taskId);
                        conditionMapper.insert(condition);
                    }
                }
            }
        }

        // 4.3 插入数据源
        List<Data> dataGroup = root.getDataGroup();
        if (dataGroup != null && !dataGroup.isEmpty()) {
            for (Data data : dataGroup) {
                if (data.getId() == null) {
                    data.setId(IdUtil.getSnowflakeNextIdStr());
                }
                data.setScheduleTaskId(taskId);
                dataMapper.insert(data);
            }
        }

        // 4.4 插入动作组和动作
        List<ActionGroup> actionGroups = root.getActionGroups();
        if (actionGroups != null && !actionGroups.isEmpty()) {
            for (ActionGroup actionGroup : actionGroups) {
                if (actionGroup.getId() == null) {
                    actionGroup.setId(IdUtil.getSnowflakeNextIdStr());
                }
                actionGroup.setScheduleTaskId(taskId);
                actionGroupMapper.insert(actionGroup);

                // 插入该动作组下的所有动作
                List<Action> actions = actionGroup.getActions();
                if (actions != null && !actions.isEmpty()) {
                    for (Action action : actions) {
                        if (action.getId() == null) {
                            action.setId(IdUtil.getSnowflakeNextIdStr());
                        }
                        action.setActionGroupId(actionGroup.getId());
                        action.setScheduleTaskId(taskId);
                        actionMapper.insert(action);
                    }
                }
            }
        }

        // 4.5 插入报警配置
        List<Alarm> alarmGroup = root.getAlarmGroup();
        if (alarmGroup != null && !alarmGroup.isEmpty()) {
            for (Alarm alarm : alarmGroup) {
                if (alarm.getId() == null) {
                    alarm.setId(IdUtil.getSnowflakeNextIdStr());
                }
                alarm.setScheduleTaskId(taskId);
                alarmMapper.insert(alarm);
            }
        }

        // 4.6 插入时间规则
        TimeRule timeRule = root.getTimeRule();
        if (timeRule != null) {
            if (timeRule.getId() == null) {
                timeRule.setId(IdUtil.getSnowflakeNextIdStr());
            }
            timeRule.setScheduleTaskId(taskId);
            timeRuleMapper.insert(timeRule);
        }

        return R.success(true, "任务更新成功");
    }
}
