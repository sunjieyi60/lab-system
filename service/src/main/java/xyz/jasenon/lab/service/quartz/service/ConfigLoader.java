package xyz.jasenon.lab.service.quartz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.jasenon.lab.common.entity.base.User;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.BaseRecord;
import xyz.jasenon.lab.service.quartz.check.Result;
import xyz.jasenon.lab.service.quartz.mapper.*;
import xyz.jasenon.lab.service.quartz.model.*;
import xyz.jasenon.lab.service.service.IUserService;
import xyz.jasenon.lab.service.strategy.device.DeviceFactory;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordFactory;
import xyz.jasenon.lab.service.vo.device.DeviceRecordVo;

import java.text.MessageFormat;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ConfigLoader {

    private final ActionGroupMapper actionGroupMapper;
    private final ActionMapper actionMapper;
    private final AlarmMapper alarmMapper;
    private final ScheduleTaskMapper scheduleTaskMapper;
    private final ConditionMapper conditionMapper;
    private final ConditionGroupMapper conditionGroupMapper;
    private final DataMapper dataMapper;
    private final TimeRuleMapper timeRuleMapper;
    private final IUserService userService;

    public List<ScheduleTask> getAllTasks(){
        return scheduleTaskMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 从数据库删除定时任务（CASCADE 会自动删除关联子表）。
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteTask(String taskId) {
        return scheduleTaskMapper.deleteById(taskId);
    }

    /**
     * 更新任务的启用状态。
     */
    public void updateTaskEnable(String taskId, Boolean enable) {
        ScheduleTask t = scheduleTaskMapper.selectById(taskId);
        if (t != null) {
            t.setEnable(enable);
            scheduleTaskMapper.updateById(t);
        }
    }

    public ScheduleConfigRoot load(String id){
        ScheduleConfigRoot scheduleConfig = new ScheduleConfigRoot();

        ScheduleTask task = scheduleTaskMapper.selectById(id);
        if (task == null){
            return null;
        }
        List<ConditionGroup> conditionGroups = conditionGroupMapper.selectList(
                new LambdaQueryWrapper<ConditionGroup>()
                        .eq(ConditionGroup::getScheduleTaskId, task.getId())
        );
        List<Condition> allConditions = conditionMapper.selectList(
                new LambdaQueryWrapper<Condition>()
                        .eq(Condition::getScheduleTaskId, task.getId())
        );
        for(ConditionGroup conditionGroup : conditionGroups){
            List<Condition> conditions = allConditions.stream()
                    .filter(c -> conditionGroup.getId().equals(c.getConditionGroupId()))
                    .toList();
            conditionGroup.setConditions(conditions);
        }
        List<Alarm> alarms = alarmMapper.selectList(
                new LambdaQueryWrapper<Alarm>()
                        .eq(Alarm::getScheduleTaskId, task.getId())
        );
        List<Data> datas = dataMapper.selectList(
                new LambdaQueryWrapper<Data>()
                        .eq(Data::getScheduleTaskId, task.getId())
        );
        TimeRule timeRule = timeRuleMapper.selectOne(
                new LambdaQueryWrapper<TimeRule>()
                        .eq(TimeRule::getScheduleTaskId, task.getId())
        );
        List<ActionGroup> actionGroups = actionGroupMapper.selectList(
                new LambdaQueryWrapper<ActionGroup>()
                        .eq(ActionGroup::getScheduleTaskId, task.getId())
        );
        List<Action> allActions = actionMapper.selectList(
                new LambdaQueryWrapper<Action>()
                        .eq(Action::getScheduleTaskId, task.getId())
        );
        for(ActionGroup actionGroup : actionGroups){
            List<Action> actions = allActions.stream()
                    .filter(a -> actionGroup.getId().equals(a.getActionGroupId()))
                    .toList();
            actionGroup.setActions(actions);
        }
        scheduleConfig.setTask(task);
        scheduleConfig.setConditionGroups(conditionGroups);
        scheduleConfig.setActionGroups(actionGroups);
        scheduleConfig.setDataGroup(datas);
        scheduleConfig.setTimeRule(timeRule);
        scheduleConfig.setAlarmGroup(alarms);
        return scheduleConfig;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> configCreate(ScheduleConfigRoot scheduleConfig) {
        Result<Boolean> verifyConfig = verifyConfig(scheduleConfig);
        if (!verifyConfig.getData()){
            return verifyConfig;
        }
        ScheduleTask task = scheduleConfig.getTask();
        scheduleTaskMapper.insert(task);
        for (ConditionGroup conditionGroup : scheduleConfig.getConditionGroups()){
            conditionGroupMapper.insert(conditionGroup);
            List<Condition> conditions = conditionGroup.getConditions();
            for (Condition condition : conditions){
                condition.setConditionGroupId(conditionGroup.getId());
                condition.setScheduleTaskId(task.getId());
                conditionMapper.insert(condition);
            }
        }
        for (Data data : scheduleConfig.getDataGroup()){
            dataMapper.insert(data);
        }
        for (ActionGroup actionGroup : scheduleConfig.getActionGroups()){
            actionGroupMapper.insert(actionGroup);
            List<Action> actions = actionGroup.getActions();
            for (Action action : actions){
                action.setActionGroupId(actionGroup.getId());
                action.setScheduleTaskId(task.getId());
                actionMapper.insert(action);
            }
        }
        for (Alarm alarm : scheduleConfig.getAlarmGroup()){
            alarmMapper.insert(alarm);
        }
        timeRuleMapper.insert(scheduleConfig.getTimeRule());
        // 注意：任务注册由调用者（如Controller）负责，避免循环依赖
        return Result.success(true);
    }

    public Result<Boolean> verifyConfig(ScheduleConfigRoot scheduleConfig){
        ScheduleTask task = scheduleConfig.getTask();

        Set<DevicePair> deviceIds = new HashSet<>();
        List<ActionGroup> actionGroups = scheduleConfig.getActionGroups();
        for (ActionGroup actionGroup : actionGroups){
            List<Action> actions = actionGroup.getActions();
            for (Action action : actions){
                deviceIds.add(new DevicePair(action.getDeviceId(), action.getDeviceType()));
            }
        }
        List<Data> datas = scheduleConfig.getDataGroup();
        for (Data data : datas){
            deviceIds.add(new DevicePair(data.getDeviceId(), data.getDeviceType()));
        }
        List<Device> devices = deviceIds.stream().map(devicePair -> {
            Device device = DeviceFactory.getDeviceQMethod(devicePair.type).getDeviceById(devicePair.deviceId);
            return device;
        }).filter(Objects::nonNull).toList();

        Result<Boolean> dataGroupCheckResult = verifyDataGroup(datas, devices,task);
        if (!dataGroupCheckResult.getData()){
            return dataGroupCheckResult;
        }

        Result<Boolean> actionGroupCheckResult = verifyActionGroup(actionGroups, devices, task);
        if (!actionGroupCheckResult.getData()){
            return actionGroupCheckResult;
        }

        List<ConditionGroup> conditionGroups = scheduleConfig.getConditionGroups();
        Result<Boolean> conditionGroupCheckResult = verifyConditionGroup(conditionGroups, datas, task);
        if (!conditionGroupCheckResult.getData()){
            return conditionGroupCheckResult;
        }

        TimeRule timeRule = scheduleConfig.getTimeRule();
        Result<Boolean> timeRuleCheckResult = verifyTimeRule(timeRule,task);
        if (!timeRuleCheckResult.getData()){
            return timeRuleCheckResult;
        }

        List<Alarm> alarms = scheduleConfig.getAlarmGroup();
        List<User> users = userService.visible();
        for (Alarm alarm : alarms){
            boolean overflow = users.stream().anyMatch(user -> user.getId().equals(alarm.getUserId()));
            if (!overflow) {
                return Result.error(false, MessageFormat.format("Alarm:{}引用的用户不归属你管辖!", alarm.getId()));
            }
        }
        Result<Boolean> verifyAlarmGroup = verifyAlarmGroup(alarms, task);
        if (!verifyAlarmGroup.getData()){
            return verifyAlarmGroup;
        }

        WatchDog watchDog = scheduleConfig.getWatchDog();
        if (watchDog != null && Boolean.TRUE.equals(watchDog.getWatchEnabled())) {
            if (watchDog.getWatchIntervalSec() <= 0) {
                return Result.error(false, MessageFormat.format("WatchDog:{} 间隔必须大于0秒", watchDog));
            }
            if (watchDog.getWatchTimeoutSec() <= 0) {
                return Result.error(false, MessageFormat.format("WatchDog:{} 超时时间必须大于0秒", watchDog));
            }
        }

        return Result.success(true);
    }

    public Result<Boolean> verifyActionGroup(List<ActionGroup> actionGroups, List<Device> devices, ScheduleTask task){
        for (ActionGroup actionGroup : actionGroups){
            if (!actionGroup.getScheduleTaskId().equals(task.getId())){
                return Result.error(false, MessageFormat.format("ActionGroup:{}不属于当前任务",actionGroup.getId()));
            }
            List<Action> actions = actionGroup.getActions();
            for (Action action : actions){
                if (!action.getActionGroupId().equals(actionGroup.getId())){
                    return Result.error(false, MessageFormat.format("Action:{}不属于当前ActionGroup",action.getId()));
                }
                if (!task.getId().equals(action.getScheduleTaskId())){
                    return Result.error(false, MessageFormat.format("Action:{}不属于当前任务",action.getId()));
                }
                Long deviceId = action.getDeviceId();
                DeviceType deviceType = action.getDeviceType();
                boolean exist = devices.stream().anyMatch(device -> device.getId().equals(deviceId) && device.getDeviceType() == deviceType);
                if (!exist){
                    return Result.error(false, MessageFormat.format("Action:{}引用的设备不存在",action.getDeviceId()));
                }
            }
        }
        return Result.success(true);
    }

    public Result<Boolean> verifyConditionGroup(List<ConditionGroup> conditionGroups,List<Data> datas, ScheduleTask task){
        for (ConditionGroup conditionGroup : conditionGroups){
            if (!conditionGroup.getScheduleTaskId().equals(task.getId())){
                return Result.error(false, MessageFormat.format("ConditionGroup:{}不属于当前任务",conditionGroup.getId()));
            }
            List<Condition> conditions = conditionGroup.getConditions();
            for (Condition condition : conditions){
                if (!condition.getConditionGroupId().equals(conditionGroup.getId())){
                    return Result.error(false, MessageFormat.format("Condition:{}不属于当前ConditionGroup",condition.getId()));
                }
                if (!task.getId().equals(condition.getScheduleTaskId())){
                    return Result.error(false, MessageFormat.format("Condition:{}不属于当前任务",condition.getId()));
                }
                String[] args = new String[2];
                String expr = condition.getExpr();
                if (expr.startsWith("#") && expr.contains(".")){
                    String[] split = expr.replace("#","").replace("{","").replace("}","").split(" ")[0].split("\\.");
                    args[0] = split[0];
                    args[1] = split[1];
                }else {
                    return Result.error(false, MessageFormat.format("Condition:{}表达式不合法",condition.getId()));
                }
                Data data = datas.stream().filter(obj->obj.getId().equals(args[0])).findAny().orElse(null);
                if (data == null){
                    return Result.error(false, MessageFormat.format("Condition:{}引用的数据:{}不存在",condition.getId(),expr));
                }
                Map<String,Object> dataMap = data.value2Map();
                boolean properties = dataMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(args[1]));
                if (!properties){
                    return Result.error(false, MessageFormat.format("Condition:{}引用的数据属性:{}不存在",condition.getId(),args[1]));
                }
            }
        }
        return Result.success(true);
    }

    public Result<Boolean> verifyDataGroup(List<Data> datas, List<Device> devices, ScheduleTask task){
        for (Data data : datas){
            if (!data.getScheduleTaskId().equals(task.getId())){
                return Result.error(false, MessageFormat.format("Data:{}不属于当前任务",data.getId()));
            }
            boolean exist = devices.stream().anyMatch(device -> device.getId().equals(data.getDeviceId()) && device.getDeviceType() == data.getDeviceType());
            if (!exist){
                return Result.error(false, MessageFormat.format("Data:{}引用的设备不存在",data.getId()));
            }
            @SuppressWarnings("unchecked")
            DeviceRecordVo<? extends BaseRecord> record = (DeviceRecordVo<? extends BaseRecord>) DeviceRecordFactory.getDeviceRecordMethod(data.getDeviceType()).getRecord(data.getDeviceId());
            if (record == null){
                return Result.error(false, MessageFormat.format("Data:{}引用的设备记录不存在",data.getId()));
            }
            if (record.getData() == null){
                return Result.error(false, MessageFormat.format("Data:{}引用的设备记录为空",data.getId()));
            }
            data.setValue(record);
        }
        return Result.success(true);
    }

    public Result<Boolean> verifyTimeRule(TimeRule timeRule, ScheduleTask task){
        if (!timeRule.getScheduleTaskId().equals(task.getId())){
            return Result.error(false, MessageFormat.format("TimeRule:{0} 不属于当前任务",timeRule.getId()));
        }
        if (timeRule.getStartWeek()<=0 && timeRule.getEndWeek()>timeRule.getStartWeek()) {
            return Result.error(false, MessageFormat.format("TimeRule:{0} 的开始周和结束周不合法", timeRule.getId()));
        }
        return Result.success(true);
    }

    public Result<Boolean> verifyAlarmGroup(List<Alarm> alarms, ScheduleTask task){
        for (Alarm alarm : alarms){
            if (!alarm.getScheduleTaskId().equals(task.getId())){
                return Result.error(false, MessageFormat.format("Alarm:{}不属于当前任务",alarm.getId()));
            }
            User user = userService.getById(alarm.getUserId());
            if (user == null){
                return Result.error(false, MessageFormat.format("Alarm:{}引用的用户不存在",alarm.getId()));
            }
            switch (alarm.getType()){
                case SMTP:
                    if (user.getEmail().isBlank()){
                        return Result.error(false, MessageFormat.format("Alarm:{}引用的用户邮箱为空",alarm.getId()));
                    }
                    break;
                case SMS:
                    if (user.getPhone().isBlank()){
                        return Result.error(false, MessageFormat.format("Alarm:{}引用的用户手机号为空",alarm.getId()));
                    }
                    break;
                default:
                    return Result.error(false, MessageFormat.format("Alarm:{}类型不合法",alarm.getId()));
            }
        }
        return Result.success(true);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public class DevicePair{
        Long deviceId;
        DeviceType type;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DevicePair devicePair = (DevicePair) o;

            if (!deviceId.equals(devicePair.deviceId)) return false;
            return type == devicePair.type;
        }

        @Override
        public int hashCode(){
            return Objects.hash(deviceId, type);
        }

    }

}
