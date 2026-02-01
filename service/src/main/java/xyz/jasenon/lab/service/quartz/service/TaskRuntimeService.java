package xyz.jasenon.lab.service.quartz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.log.AlarmLog;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.log.LogTaskManager;
import xyz.jasenon.lab.service.quartz.check.ConditionExprChecker;
import xyz.jasenon.lab.service.quartz.check.DataCollector;
import xyz.jasenon.lab.service.quartz.check.Result;
import xyz.jasenon.lab.service.quartz.check.TimeRuleChecker;
import xyz.jasenon.lab.service.quartz.config.QuartzRegister;
import xyz.jasenon.lab.service.quartz.model.*;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import xyz.jasenon.lab.service.strategy.device.DeviceFactory;
import xyz.jasenon.lab.service.strategy.task.TaskDispatch;
import org.quartz.SchedulerException;

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
    private final QuartzRegister quartzRegister;
    private final TimeRuleChecker timeRuleChecker;
    private final ExecutorService taskRuntimeExecutor;
    private final ScheduledExecutorService watchDogScheduler;
    private final LogTaskManager logTaskManager;
    private final ILaboratoryService laboratoryService;

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
                logConditionAlarm(cfg, "时间规则异常", timeRuleCheckResult.getMessage());
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
                // 条件触发：数据源检查失败
                logConditionAlarm(cfg, "数据源异常", dataOriginCheckResult.getMessage());
                // 设备异常：离线/数据不可用，记录设备异常报警
                logDeviceException(cfg, data, dataOriginCheckResult.getMessage());
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
                    logConditionAlarm(cfg, "条件表达式异常",
                            condition.getExpr() + " 表达式评估失败，错误信息：" + conditionCheckResult.getMessage());
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
                logConditionAlarm(cfg, "条件不满足",
                        "条件组 " + actionGroup.getConditionGroupId() + " 不满足，不执行动作");
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

    /**
     * 条件触发类报警：时间规则、数据源、条件表达式等异常。
     * category 固定为「条件触发」，alarmType 用于区分具体类型。
     */
    private void logConditionAlarm(ScheduleConfigRoot cfg, String alarmType, String errorMessage) {
        ScheduleTask task = cfg.getTask();
        String taskId = task != null ? task.getId() : null;
        log.warn("条件触发报警, taskId={} -> {}", taskId, errorMessage);

        String room = null;
        if (task != null && task.getLaboratoryId() != null) {
            try {
                Laboratory lab = laboratoryService.getById(task.getLaboratoryId());
                if (lab != null) {
                    room = lab.getLaboratoryName() != null ? lab.getLaboratoryName() : lab.getLaboratoryId();
                } else {
                    room = String.valueOf(task.getLaboratoryId());
                }
            } catch (Exception e) {
                log.debug("查询实验室信息失败, laboratoryId={}", task.getLaboratoryId(), e);
            }
        }

        // 设备摘要：从动作组中汇总涉及的设备，便于按设备维度检索
        String deviceSummary = null;
        try {
            List<ActionGroup> actionGroups = cfg.getActionGroups();
            if (actionGroups != null) {
                StringBuilder sb = new StringBuilder();
                for (ActionGroup ag : actionGroups) {
                    if (ag.getActions() == null) continue;
                    for (Action a : ag.getActions()) {
                        if (sb.length() > 0) {
                            sb.append("; ");
                        }
                        sb.append(a.getDeviceType()).append("-").append(a.getDeviceId());
                    }
                }
                deviceSummary = sb.length() > 0 ? sb.toString() : null;
            }
        } catch (Exception e) {
            log.debug("汇总条件触发报警设备列表失败, taskId={}", taskId, e);
        }

        AlarmLog alarmLog = new AlarmLog()
                .setCategory("条件触发")
                .setAlarmType(alarmType)
                .setRoom(room)
                .setDevice(deviceSummary)
                .setContent("taskId=" + taskId + " -> " + errorMessage)
                .setAlarmTime(LocalDateTime.now());
        logTaskManager.submitAlarmLog(alarmLog);
    }

    /**
     * 设备异常类报警：例如数据源离线（设备或网关掉线），category=设备异常。
     */
    private void logDeviceException(ScheduleConfigRoot cfg, Data data, String errorMessage) {
        ScheduleTask task = cfg.getTask();
        String taskId = task != null ? task.getId() : null;

        String room = null;
        String deviceName = null;
        try {
            Device device = DeviceFactory.getDeviceQMethod(data.getDeviceType()).getDeviceById(data.getDeviceId());
            if (device != null) {
                deviceName = device.getDeviceName();
                Long labId = device.getBelongToLaboratoryId();
                if (labId != null) {
                    Laboratory lab = laboratoryService.getById(labId);
                    room = lab != null
                            ? (lab.getLaboratoryName() != null ? lab.getLaboratoryName() : lab.getLaboratoryId())
                            : String.valueOf(labId);
                }
            }
        } catch (Exception e) {
            log.debug("解析设备异常报警的教室/设备失败, taskId={}, dataId={}", taskId, data.getId(), e);
        }

        String alarmType;
        DeviceType deviceType = data.getDeviceType();
        if (deviceType == null) {
            alarmType = "设备异常";
        } else {
            switch (deviceType) {
                case AirCondition -> alarmType = "空调掉线";
                case CircuitBreak -> alarmType = "电气设备掉线";
                case Sensor -> alarmType = "环境传感器掉线";
                case Access -> alarmType = "门禁掉线";
                case Light -> alarmType = "照明设备掉线";
                default -> alarmType = "设备异常";
            }
        }

        AlarmLog alarmLog = new AlarmLog()
                .setCategory("设备异常")
                .setAlarmType(alarmType)
                .setRoom(room)
                .setDevice(deviceName)
                .setContent("taskId=" + taskId + ", dataId=" + data.getId() + " -> " + errorMessage)
                .setAlarmTime(LocalDateTime.now());
        logTaskManager.submitAlarmLog(alarmLog);
    }

    /**
     * 获取定时任务列表。enable 为空/null 时返回全部，为 "1" 仅启用，为 "0" 仅禁用。
     */
    public List<ScheduleTask> getAllScheduleTask(String enable) {
        return configLoader.getAllTasks(enable);
    }

    /**
     * 删除定时任务（从数据库物理删除）。成功返回 null，失败返回错误提示文案。
     */
    public String deleteTask(String taskId) {
        if (configLoader.load(taskId) == null) {
            return "任务不存在";
        }
        try {
            quartzRegister.cancelTask(taskId);
        } catch (SchedulerException e) {
            log.warn("删除前移除 Quartz 任务失败, taskId={}", taskId, e);
        }
        int deleted = configLoader.deleteTask(taskId);
        return deleted > 0 ? null : "任务不存在";
    }

    /**
     * 取消定时任务。成功返回 null，失败返回错误提示文案。
     */
    public String cancelTask(String taskId) {
        ScheduleConfigRoot cfg = configLoader.load(taskId);
        if (cfg == null) {
            return "任务不存在";
        }
        if ("0".equals(cfg.getTask().getEnable())) {
            return "任务已是取消状态";
        }
        configLoader.updateTaskEnable(taskId, "0");
        try {
            quartzRegister.cancelTask(taskId);
        } catch (SchedulerException e) {
            log.warn("取消 Quartz 任务失败, taskId={}", taskId, e);
        }
        return null;
    }

    /**
     * 启用定时任务。成功返回 null，失败返回错误提示文案。
     */
    public String enableTask(String taskId) {
        ScheduleConfigRoot cfg = configLoader.load(taskId);
        if (cfg == null) {
            return "任务不存在";
        }
        if ("1".equals(cfg.getTask().getEnable())) {
            return "任务已是启用状态";
        }
        configLoader.updateTaskEnable(taskId, "1");
        try {
            quartzRegister.scheduleTask(cfg);
        } catch (SchedulerException e) {
            log.warn("启用 Quartz 任务失败, taskId={}", taskId, e);
        }
        return null;
    }
}
