package xyz.jasenon.lab.service.quartz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.quartz.check.ConditionExprChecker;
import xyz.jasenon.lab.service.quartz.check.DataCollector;
import xyz.jasenon.lab.service.quartz.check.Result;
import xyz.jasenon.lab.service.quartz.check.TimeRuleChecker;
import xyz.jasenon.lab.service.quartz.model.*;
import xyz.jasenon.lab.service.strategy.task.TaskDispatch;
import xyz.jasenon.lab.service.vo.DeviceRecordVo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TaskRuntimeService {

    public final ConfigLoader configLoader;
    private final TimeRuleChecker timeRuleChecker;

    public void runOnce(String scheduleTaskId){
        // 执行一次任务
        // 1. 获取任务配置
        // 2. 根据任务配置执行任务
        // 3. 更新任务状态为已完成
        ScheduleConfigRoot cfg = configLoader.load(scheduleTaskId);

        TimeRule timeRule = cfg.getTimeRule();
        Result<Boolean> timeRuleCheckResult = timeRuleChecker.check(timeRule);
        if (!timeRuleCheckResult.getData()){
            logError(timeRuleCheckResult.getMessage());
            return;
        }

        Map<String, Map<String,Object>> dataMap = new HashMap<>();
        List<Data> datas = cfg.getDataGroup();
        for (Data data : datas) {
            // 收集数据
            DataCollector.collect(data);
            Result<Boolean> dataOriginCheckResult = DataCollector.check(data);
            if (!dataOriginCheckResult.getData()){
                logError(dataOriginCheckResult.getMessage());
                return;
            }
            dataMap.put(data.getId(), data.value2Map());
        }

        Map<String,Boolean> conditionResultMap = new HashMap<>();
        List<ConditionGroup> conditionGroups = cfg.getConditionGroups();
        for (ConditionGroup conditionGroup : conditionGroups) {
            List<Condition> conditions = conditionGroup.getConditions();
            for (Condition condition : conditions) {
                Result<Boolean> conditionCheckResult = ConditionExprChecker.eval(condition.getExpr(), dataMap);
                if (!conditionCheckResult.getData()){
                    logError(condition.getExpr() + " 表达式评估失败，错误信息：" + conditionCheckResult.getMessage());
                }
                switch (conditionGroup.getType()){
                    case ALL:
                    {
                        Boolean now = conditionResultMap.getOrDefault(conditionGroup.getId(), conditionCheckResult.getData());
                        now = now && conditionCheckResult.getData();
                        conditionResultMap.put(conditionGroup.getId(), now);
                    }
                    case ANY:
                    {
                        Boolean now = conditionResultMap.getOrDefault(conditionGroup.getId(), !conditionCheckResult.getData());
                        now = now || conditionCheckResult.getData();
                        conditionResultMap.put(conditionGroup.getId(), now);
                    }
                }
            }
        }

        List<ActionGroup> actionGroups = cfg.getActionGroups();
        for (ActionGroup actionGroup : actionGroups) {
            Boolean conditionGroupResult = conditionResultMap.getOrDefault(actionGroup.getConditionGroupId(),false);
            if (!conditionGroupResult){
                logError("条件组 " + actionGroup.getConditionGroupId() + " 不满足，不执行动作");
                continue;
            }
            List<Action> actions = actionGroup.getActions();
            for (Action action : actions) {
                Task task = action.convert2Task();
                TaskDispatch.dispatch(task);
            }
        }

        List<Alarm> alarms = cfg.getAlarmGroup();
        // todo 发送报警信息

    }

    public R createTask(ScheduleConfigRoot cfg){
        Result<Boolean> result = configLoader.configCreate(cfg);
        if (!result.getData()){
            return R.fail(result.getMessage());
        }

        return R.success("创建成功");
    }

    private void logError(String errorMessage){
        // 记录错误日志
    }

    public List<ScheduleTask> getAllScheduleTask(){
        return configLoader.getAllTasks();
    }

}
