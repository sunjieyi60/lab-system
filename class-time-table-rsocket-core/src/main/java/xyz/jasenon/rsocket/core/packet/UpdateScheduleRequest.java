package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * 更新课表请求
 * 
 * 服务器推送新课表数据
 */
@Getter
@Setter
public class UpdateScheduleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 课表版本号
     */
    private Long scheduleVersion;

    /**
     * 课表数据（简化表示，实际应为课程列表）
     */
    private List<CourseSchedule> schedules;

    /**
     * 生效时间
     */
    private Instant effectiveTime;

    /**
     * 请求时间
     */
    private Instant requestTime;

    /**
     * 课表项
     */
    @Getter
    @Setter
    public static class ScheduleItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String courseName;
        private String teacherName;
        private String startTime;
        private String endTime;
        private List<Integer> weekdays;
    }
}
