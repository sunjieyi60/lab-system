package xyz.jasenon.lab.service.quartz.check;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.service.mapper.SemesterMapper;
import xyz.jasenon.lab.service.quartz.model.TimeRule;

import java.time.LocalDateTime;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Component
public class TimeRuleChecker {

    @Autowired
    private SemesterMapper semesterMapper;

    public Result<Boolean> check(TimeRule timeRule){
        LocalDateTime now = LocalDateTime.now();

        /*
         * 检查时间是否在当前时间段内
         */
        if (now.toLocalTime().isBefore(timeRule.getStartTime()) || now.toLocalTime().isAfter(timeRule.getEndTime())) {
            return Result.error(false, "不在时间段内");
        }

        /*
         * 检查星期是否在当前周内
         */
        int weekday = now.getDayOfWeek().getValue();
        if (!timeRule.getWeekdays().contains(weekday)) {
            return Result.error(false, "不是对应星期");
        }

        /*
         * 检查开始周和结束周是否在当前学期内
         */
        boolean needCheckStartAndEndWeek = timeRule.getSemesterId() != null;
        if (needCheckStartAndEndWeek) {
            Semester semester = semesterMapper.selectById(timeRule.getSemesterId());
            if (semester == null) {
                return Result.error(false, "学期不存在");
            }

            int passedDays = now.getDayOfYear() - semester.getStartDate().getDayOfYear();
            int passedWeeks = passedDays % 7 >0 ? passedDays / 7 + 1 : passedDays / 7;
            if (passedWeeks < timeRule.getStartWeek() || passedWeeks > timeRule.getEndWeek()) {
                return Result.error(false, "不在学期内");
            }

            switch (timeRule.getWeekType()){
                case Single -> {
                    if (passedWeeks % 2 == 0) {
                        return Result.error(false, "当前周次不是单周");
                    }
                }
                case Double -> {
                    if (passedWeeks % 2 != 0) {
                        return Result.error(false, "当前周次不是双周");
                    }
                }
                case Both -> {}
            }
        }

        return Result.success(true);
    }

}
