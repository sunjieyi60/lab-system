package xyz.jasenon.lab.service.quartz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.log.AlarmLog;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.log.LogTaskManager;
import xyz.jasenon.lab.service.quartz.check.ConditionExprChecker;
import xyz.jasenon.lab.service.quartz.check.DataCollector;
import xyz.jasenon.lab.service.quartz.check.Result;
import xyz.jasenon.lab.service.quartz.check.TimeRuleChecker;
import xyz.jasenon.lab.service.quartz.model.*;
import xyz.jasenon.lab.service.strategy.task.TaskDispatch;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskRuntimeService {

    public final ConfigLoader configLoader;
    private final TimeRuleChecker timeRuleChecker;
    private final ExecutorService taskRuntimeExecutor;
    private final ScheduledExecutorService watchDogScheduler;
    private final LogTaskManager logTaskManager;

    /**
     * 任务入口：提交到线程池异步执行，避免阻塞Quartz调度线程。
     */
    public void submit(String scheduleTaskId) {
        taskRuntimeExecutor.submit(() -> safeExecute(scheduleTaskId));
    }

    /**
     * 同步执行（便于测试或外部直接调用）。
     */
    public void runOnce(String scheduleTaskId) {
        safeExecute(scheduleTaskId);
    }

    private void safeExecute(String scheduleTaskId) {
        try {
            executeWithWatchDog(scheduleTaskId);
        } catch (Exception ex) {
            log.error("任务执行异常, taskId={}", scheduleTaskId, ex);
        }
    }

    private void executeWithWatchDog(String scheduleTaskId) {
        ScheduleConfigRoot cfg = configLoader.load(scheduleTaskId);
        if (cfg == null || cfg.getTask() == null) {
            log.warn("未找到调度任务配置, taskId={}", scheduleTaskId);
            return;
        }
        WatchDog watchDog = cfg.getWatchDog();
        if (watchDog == null || Boolean.FALSE.equals(watchDog.getWatchEnabled())) {
            runOnceInternal(cfg);
            return;
        }

        Instant start = Instant.now();
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
        Runnable runner = () -> {
            boolean success = runOnceInternal(cfg);
            if (success && Boolean.TRUE.equals(watchDog.getStopOnFirstSuccess())) {
                ScheduledFuture<?> f = futureRef.get();
                if (f != null) {
                    f.cancel(false);
                }
            }
            if (Duration.between(start, Instant.now()).getSeconds() >= watchDog.getWatchTimeoutSec()) {
                ScheduledFuture<?> f = futureRef.get();
                if (f != null && !f.isDone()) {
                    log.warn("任务看门狗超时, taskId={}", cfg.getTask().getId());
                    f.cancel(false);
                }
            }
        };
        ScheduledFuture<?> future = watchDogScheduler.scheduleAtFixedRate(
                runner,
                0,
                Math.max(1, watchDog.getWatchIntervalSec()),
                TimeUnit.SECONDS
        );
        futureRef.set(future);

        watchDogScheduler.schedule(() -> {
            ScheduledFuture<?> f = futureRef.get();
            if (f != null && !f.isDone()) {
                log.warn("任务看门狗超时强制结束, taskId={}", cfg.getTask().getId());
                f.cancel(false);
            }
        }, watchDog.getWatchTimeoutSec(), TimeUnit.SECONDS);
    }

    private boolean runOnceInternal(ScheduleConfigRoot cfg) {
        TimeRule timeRule = cfg.getTimeRule();
        if (timeRule != null) {
            Result<Boolean> timeRuleCheckResult = timeRuleChecker.check(timeRule);
            if (!timeRuleCheckResult.getData()) {
                logError(cfg.getTask().getId(), timeRuleCheckResult.getMessage());
                return false;
            }
        }

        Map<String, Map<String, Object>> dataMap = new HashMap<>();
        List<Data> datas = cfg.getDataGroup();
        for (Data data : datas) {
            // 收集数据
            DataCollector.collect(data);
            Result<Boolean> dataOriginCheckResult = DataCollector.check(data);
            if (!dataOriginCheckResult.getData()) {
                logError(cfg.getTask().getId(), dataOriginCheckResult.getMessage());
                continue;
            }
            dataMap.put(data.getId(), data.value2Map());
        }

        Map<String, Boolean> conditionResultMap = new HashMap<>();
        List<ConditionGroup> conditionGroups = cfg.getConditionGroups();
        for (ConditionGroup conditionGroup : conditionGroups) {
            List<Condition> conditions = conditionGroup.getConditions();
            for (Condition condition : conditions) {
                Result<Boolean> conditionCheckResult = ConditionExprChecker.eval(condition.getExpr(), dataMap);
                if (!conditionCheckResult.getData()) {
                    logError(cfg.getTask().getId(), condition.getExpr() + " 表达式评估失败，错误信息：" + conditionCheckResult.getMessage());
                }
                switch (conditionGroup.getType()) {
                    case ALL -> {
                        Boolean now = conditionResultMap.getOrDefault(conditionGroup.getId(), conditionCheckResult.getData());
                        now = now && conditionCheckResult.getData();
                        conditionResultMap.put(conditionGroup.getId(), now);
                    }
                    case ANY -> {
                        Boolean now = conditionResultMap.getOrDefault(conditionGroup.getId(), !conditionCheckResult.getData());
                        now = now || conditionCheckResult.getData();
                        conditionResultMap.put(conditionGroup.getId(), now);
                    }
                    default -> throw new RuntimeException("未知的条件组类型");
                }
            }
        }

        boolean hasActionExecuted = false;
        List<ActionGroup> actionGroups = cfg.getActionGroups();
        for (ActionGroup actionGroup : actionGroups) {
            Boolean conditionGroupResult = conditionResultMap.getOrDefault(actionGroup.getConditionGroupId(), true);
            if (!conditionGroupResult) {
                logError(cfg.getTask().getId(), "条件组 " + actionGroup.getConditionGroupId() + " 不满足，不执行动作");
                continue;
            }
            List<Action> actions = actionGroup.getActions();
            for (Action action : actions) {
                Task task = action.convert2Task();
                TaskDispatch.dispatch(task);
                hasActionExecuted = true;
            }
        }

        // todo 发送报警信息 cfg.getAlarmGroup()
        return hasActionExecuted;
    }

    public R<String> createTask(ScheduleConfigRoot cfg) {
        Result<Boolean> result = configLoader.configCreate(cfg);
        if (!result.getData()) {
            return R.fail(result.getMessage());
        }

        return R.success("创建成功");
    }

    private void logError(String taskId, String errorMessage) {
        log.warn("taskId={} -> {}", taskId, errorMessage);

        // 条件/数据源/时间规则异常，按“条件触发报警”写一条报警日志，后续可按需扩充教室、设备等信息。
        AlarmLog alarmLog = new AlarmLog()
                .setCategory("条件触发")
                .setContent("taskId=" + taskId + " -> " + errorMessage)
                .setAlarmTime(LocalDateTime.now());
        logTaskManager.submitAlarmLog(alarmLog);
    }

    public List<ScheduleTask> getAllScheduleTask() {
        return configLoader.getAllTasks();
    }

}
