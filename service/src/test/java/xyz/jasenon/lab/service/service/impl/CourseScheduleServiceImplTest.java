package xyz.jasenon.lab.service.service.impl;

import org.junit.jupiter.api.Test;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;

import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课程课表服务测试：重点验证冲突检测算法
 */
class CourseScheduleServiceImplTest {

    private boolean invokeHasConflict(CourseScheduleServiceImpl service, CourseSchedule a, CourseSchedule b) throws Exception {
        Method m = CourseScheduleServiceImpl.class.getDeclaredMethod("hasConflict", CourseSchedule.class, CourseSchedule.class);
        m.setAccessible(true);
        return (boolean) m.invoke(service, a, b);
    }

    @Test
    void testConflict_sameTeacher_timeOverlap() throws Exception {
        CourseScheduleServiceImpl service = new CourseScheduleServiceImpl();
        CourseSchedule a = new CourseSchedule();
        a.setLaboratoryId(1L);
        a.setTeacherId(10L);
        a.setWeekType(WeekType.Both);
        a.setStartWeek(1);
        a.setEndWeek(16);
        a.setStartTime(LocalTime.of(8, 0));
        a.setEndTime(LocalTime.of(9, 40));
        a.setWeekdays(List.of(1, 3));

        CourseSchedule b = new CourseSchedule();
        b.setLaboratoryId(2L); // 不同实验室
        b.setTeacherId(10L);   // 同教师
        b.setWeekType(WeekType.Both);
        b.setStartWeek(8);
        b.setEndWeek(12);
        b.setStartTime(LocalTime.of(9, 0));
        b.setEndTime(LocalTime.of(10, 0));
        b.setWeekdays(List.of(3));

        assertTrue(invokeHasConflict(service, a, b));
    }

    @Test
    void testNoConflict_differentLabAndTeacher() throws Exception {
        CourseScheduleServiceImpl service = new CourseScheduleServiceImpl();
        CourseSchedule a = new CourseSchedule();
        a.setLaboratoryId(1L);
        a.setTeacherId(10L);
        a.setWeekType(WeekType.Single);
        a.setStartWeek(1);
        a.setEndWeek(15);
        a.setStartTime(LocalTime.of(8, 0));
        a.setEndTime(LocalTime.of(9, 0));
        a.setWeekdays(List.of(2));

        CourseSchedule b = new CourseSchedule();
        b.setLaboratoryId(2L);
        b.setTeacherId(11L);
        b.setWeekType(WeekType.Double);
        b.setStartWeek(2);
        b.setEndWeek(16);
        b.setStartTime(LocalTime.of(9, 0));
        b.setEndTime(LocalTime.of(10, 0));
        b.setWeekdays(List.of(3));

        assertFalse(invokeHasConflict(service, a, b));
    }
}