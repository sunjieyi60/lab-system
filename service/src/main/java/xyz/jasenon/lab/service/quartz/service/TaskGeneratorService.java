package xyz.jasenon.lab.service.quartz.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jasenon.lab.common.entity.class_time_table.Course;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.mapper.CourseMapper;
import xyz.jasenon.lab.service.mapper.CourseScheduleMapper;
import xyz.jasenon.lab.service.mapper.SemesterMapper;
import xyz.jasenon.lab.service.quartz.mapper.*;
import xyz.jasenon.lab.service.quartz.model.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskGeneratorService {

    private final ScheduleTaskMapper scheduleTaskMapper;
    private final TimeRuleMapper timeRuleMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;

    // 智能控制模板复制所需 Mapper
    private final ConditionGroupMapper conditionGroupMapper;
    private final ConditionMapper conditionMapper;
    private final ActionGroupMapper actionGroupMapper;
    private final ActionMapper actionMapper;
    private final DataMapper dataMapper;

    /**
     * 根据课表生成定时任务
     *
     * @param target 生成任务的目标配置
     * @return 生成结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> generateScheduleTask(CourseScheduleTaskGenerator target) {

        if (target.getLaboratoryId().isEmpty()) {
            throw R.badRequest("实验室id不能为空").convert();
        }

        List<Long> laboratoryIds = target.getLaboratoryId();
        Integer earlyStart = target.getEarlyStart();
        Integer delayEnd = target.getDelayEnd();
        String cron = target.getCron();
        Boolean enable = target.getEnable();

        // 查询指定实验室的所有课表
        List<CourseSchedule> courseSchedules = courseScheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .in(CourseSchedule::getLaboratoryId, laboratoryIds)
                        .eq(CourseSchedule::getSemesterId, target.getSemesterId())
        );

        if (courseSchedules.isEmpty()) {
            return R.success(true, "没有找到课表数据");
        }

        for (CourseSchedule courseSchedule : courseSchedules) {
            // 获取学期信息以确定任务的开始和结束日期
            Long semesterId = courseSchedule.getSemesterId();
            Semester semester = semesterMapper.selectById(semesterId);
            if (semester == null) {
                continue;
            }

            // 生成任务主体
            ScheduleTask scheduleTask = new ScheduleTask();
            String taskId = IdUtil.getSnowflakeNextIdStr();
            scheduleTask.setId(taskId);
            scheduleTask.setTaskName(generateTaskName(courseSchedule));
            scheduleTask.setCron(cron);
            scheduleTask.setEnable(enable);
            scheduleTask.setStartDate(semester.getStartDate());
            scheduleTask.setEndDate(semester.getEndDate());
            scheduleTask.setLaboratoryId(courseSchedule.getLaboratoryId());

            // 使用时间规则转换方法，并应用提前/延迟设置
            TimeRule timeRule = TimeRule.courseSchedule2TimeRule(courseSchedule);
            timeRule.setScheduleTaskId(taskId);

            // 应用提前开始设置（上课前 earlyStart 分钟）
            if (earlyStart != null && earlyStart > 0) {
                LocalTime adjustedStartTime = courseSchedule.getStartTime().minusMinutes(earlyStart);
                timeRule.setStartTime(adjustedStartTime);
            }

            // 应用延迟结束设置（下课后 delayEnd 分钟）
            if (delayEnd != null && delayEnd > 0) {
                LocalTime adjustedEndTime = courseSchedule.getEndTime().plusMinutes(delayEnd);
                timeRule.setEndTime(adjustedEndTime);
            }

            // 保存任务和时间规则
            scheduleTaskMapper.insert(scheduleTask);
            timeRuleMapper.insert(timeRule);

            // 深度复制智能控制模板（条件组、数据源、动作组）
            copySmartControlTemplate(target, taskId);
        }

        return R.success(true, "成功生成 " + courseSchedules.size() + " 个定时任务");
    }

    /**
     * 深度复制智能控制模板到指定任务
     * <p>
     * 核心规则：
     * 1. 所有实体重新生成 Snowflake ID，避免与模板或已生成任务冲突
     * 2. 条件组先复制，建立【旧ID → 新ID】映射
     * 3. 动作组复制时，通过映射表还原与条件组的原始关联关系
     * 4. 所有子项的 scheduleTaskId 统一指向新任务ID
     *
     * @param template 课表任务生成器（携带智能控制模板）
     * @param taskId   新生成的任务ID
     */
    private void copySmartControlTemplate(CourseScheduleTaskGenerator template, String taskId) {
        // ---------- 1. 先复制数据源（建立 dataId 映射表，供条件表达式替换使用） ----------
        Map<String, String> dataIdMap = new HashMap<>();
        List<Data> dataGroup = template.getDataGroup();
        if (dataGroup != null && !dataGroup.isEmpty()) {
            for (Data data : dataGroup) {
                String oldDataId = data.getId();
                String newDataId = IdUtil.getSnowflakeNextIdStr();
                if (oldDataId != null) {
                    dataIdMap.put(oldDataId, newDataId);
                }

                Data newData = new Data();
                BeanUtil.copyProperties(data, newData, "id", "scheduleTaskId");
                newData.setId(newDataId);
                newData.setScheduleTaskId(taskId);
                dataMapper.insert(newData);
            }
        }

        // ---------- 2. 复制条件组（使用 dataIdMap 替换 expr 中的数据源引用） ----------
        Map<String, String> conditionGroupIdMap = new HashMap<>();
        List<ConditionGroup> conditionGroups = template.getConditionGroups();
        if (conditionGroups != null && !conditionGroups.isEmpty()) {
            for (ConditionGroup group : conditionGroups) {
                String oldGroupId = group.getId();
                String newGroupId = IdUtil.getSnowflakeNextIdStr();
                if (oldGroupId != null) {
                    conditionGroupIdMap.put(oldGroupId, newGroupId);
                }

                ConditionGroup newGroup = new ConditionGroup();
                BeanUtil.copyProperties(group, newGroup, "id", "scheduleTaskId", "conditions");
                newGroup.setId(newGroupId);
                newGroup.setScheduleTaskId(taskId);
                conditionGroupMapper.insert(newGroup);

                List<Condition> conditions = group.getConditions();
                if (conditions != null && !conditions.isEmpty()) {
                    for (Condition condition : conditions) {
                        Condition newCondition = new Condition();
                        BeanUtil.copyProperties(condition, newCondition,
                                "id", "conditionGroupId", "scheduleTaskId", "expr");
                        newCondition.setId(IdUtil.getSnowflakeNextIdStr());
                        newCondition.setConditionGroupId(newGroupId);
                        newCondition.setScheduleTaskId(taskId);

                        // 关键：替换表达式 #{oldDataId} 为 #{newDataId}
                        String newExpr = remapDataIdsInExpr(condition.getExpr(), dataIdMap);
                        newCondition.setExpr(newExpr);

                        conditionMapper.insert(newCondition);
                    }
                }
            }
        }

        // ---------- 3. 复制动作组（使用映射表还原 conditionGroupId 关联） ----------
        List<ActionGroup> actionGroups = template.getActionGroups();
        if (actionGroups != null && !actionGroups.isEmpty()) {
            for (ActionGroup group : actionGroups) {
                ActionGroup newGroup = new ActionGroup();
                BeanUtil.copyProperties(group, newGroup, "id", "scheduleTaskId", "conditionGroupId", "actions");
                newGroup.setId(IdUtil.getSnowflakeNextIdStr());
                newGroup.setScheduleTaskId(taskId);

                // 关键：通过映射表还原动作组与条件组的原始关联
                String originalConditionGroupId = group.getConditionGroupId();
                if (originalConditionGroupId != null) {
                    String mappedConditionGroupId = conditionGroupIdMap.get(originalConditionGroupId);
                    newGroup.setConditionGroupId(mappedConditionGroupId);
                }
                actionGroupMapper.insert(newGroup);

                List<Action> actions = group.getActions();
                if (actions != null && !actions.isEmpty()) {
                    for (Action action : actions) {
                        Action newAction = new Action();
                        BeanUtil.copyProperties(action, newAction, "id", "actionGroupId", "scheduleTaskId");
                        newAction.setId(IdUtil.getSnowflakeNextIdStr());
                        newAction.setActionGroupId(newGroup.getId());
                        newAction.setScheduleTaskId(taskId);
                        actionMapper.insert(newAction);
                    }
                }
            }
        }
    }

    /**
     * 替换条件表达式中的数据源ID引用
     * <p>
     * 表达式中的 dataId 即为 Data.java 的主键 ID（数据源ID，雪花算法生成的纯数字字符串）
     * 例如：#{100}.isOpen == false 中的 100 需要替换为新生成的 dataId
     *
     * @param expr       原始表达式
     * @param dataIdMap  旧dataId → 新dataId 映射表
     * @return 替换后的表达式
     */
    private String remapDataIdsInExpr(String expr, Map<String, String> dataIdMap) {
        if (expr == null || dataIdMap.isEmpty()) {
            return expr;
        }
        String result = expr;
        for (Map.Entry<String, String> entry : dataIdMap.entrySet()) {
            String oldId = entry.getKey();
            String newId = entry.getValue();
            // 精确替换 #{oldId} 为 #{newId}
            if (result.contains(entry.getKey())){
                result = result.replace(entry.getKey(),entry.getValue());
            };
        }
        return result;
    }

    /**
     * 生成任务名称
     *
     * @param courseSchedule 课表
     * @return 任务名称
     */
    private String generateTaskName(CourseSchedule courseSchedule) {
        // 查询课程名称
        Course course = courseMapper.selectById(courseSchedule.getCourseId());
        String courseName = course != null ? course.getCourseName() : "未知课程";

        return String.format("%s-实验室%d-第%d-%d周-第%d-%d节-%s",
                courseName,
                courseSchedule.getLaboratoryId(),
                courseSchedule.getStartWeek(),
                courseSchedule.getEndWeek(),
                courseSchedule.getStartSection(),
                courseSchedule.getEndSection(),
                formatWeekdays(courseSchedule.getWeekdays()));
    }

    /**
     * 格式化星期列表
     *
     * @param weekdays 星期列表 (1-7 代表周一至周日)
     * @return 格式化字符串
     */
    private String formatWeekdays(List<Integer> weekdays) {
        if (weekdays == null || weekdays.isEmpty()) {
            return "";
        }
        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        StringBuilder sb = new StringBuilder();
        for (Integer day : weekdays) {
            if (day >= 1 && day <= 7) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(dayNames[day]);
            }
        }
        return sb.toString();
    }
}
