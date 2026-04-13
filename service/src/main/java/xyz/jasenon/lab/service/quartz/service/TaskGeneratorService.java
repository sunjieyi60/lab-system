package xyz.jasenon.lab.service.quartz.service;

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
import xyz.jasenon.lab.service.quartz.mapper.ScheduleTaskMapper;
import xyz.jasenon.lab.service.quartz.mapper.TimeRuleMapper;
import xyz.jasenon.lab.service.quartz.model.CourseScheduleTaskGenerator;
import xyz.jasenon.lab.service.quartz.model.ScheduleTask;
import xyz.jasenon.lab.service.quartz.model.TimeRule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskGeneratorService {

    private final ScheduleTaskMapper scheduleTaskMapper;
    private final TimeRuleMapper timeRuleMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;

    /**
     * 根据课表生成定时任务
     *
     * @param target 生成任务的目标配置
     * @return 生成结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> generateScheduleTask(CourseScheduleTaskGenerator target) {

        if (target.getLaboratoryId().isEmpty()){
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
        }

        return R.success(true, "成功生成 " + courseSchedules.size() + " 个定时任务");
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
