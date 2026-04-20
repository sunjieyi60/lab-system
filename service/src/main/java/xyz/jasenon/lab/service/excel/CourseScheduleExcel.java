package xyz.jasenon.lab.service.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.jasenon.lab.common.entity.class_time_table.Course;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.common.entity.class_time_table.Teacher;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;
import xyz.jasenon.lab.service.mapper.CourseMapper;
import xyz.jasenon.lab.service.mapper.TeacherMapper;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseScheduleExcel {

    /**
     * 课程名称
     */
    private String name;           // 课程名
    /**
     * 教师信息
     */
    private String teacher;
    /**
     * 时间信息
     */
    private String timeInfo;
    private Integer startWeek;
    private Integer endWeek;
    /**
     * 课程周次类型
     */
    private String weekType;

    /**
     * 错误信息定位用
     */
    private Integer columnIndex;
    private Integer rowIndex;

    /**
     * 原始内容，用于错误回显
     */
    private String rawContent;


    /**
     * 获取处理器
     * @return 返回处理器
     */
    public Handler handle(TeacherMapper teacherMapper, CourseMapper courseMapper) {
        Handler handler = new Handler();
        handler.teacherMapper = teacherMapper;
        handler.courseMapper = courseMapper;
        return handler;
    }

    public class Handler implements CourseSchedule.Convert {

        private TeacherMapper teacherMapper;
        private CourseMapper courseMapper;

        /**
         * 周次信息
         */
        private WeekType weekType;

        /**
         * 节次信息
         */
        private Integer startSection;
        private Integer endSection;

        /**
         * 时间信息
         */
        private LocalTime startTime;
        private LocalTime endTime;

        /**
         * 星期信息
         */
        private Integer dayOfWeek;

        private Course courseInfo;

        private Teacher teacherInfo;

        /**
         * 是否成功
         */
        private Boolean success = true;
        /**
         * 错误信息 本次解析的错误信息
         */
        private List<ExcelParseError> errors = new ArrayList<>();

        private static final Pattern SECTION_PATTERN = Pattern.compile("(\\d+)-(\\d+)节");
        private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+):(\\d+)-(\\d+):(\\d+)");

        @Override
        public CourseSchedule.CourseScheduleHandle convert(Long semesterId, Long laboratoryId) {
            handleWeekType();
            handleDayOfWeek();
            handleTimeInfo();
            handleTeacherInfo();
            handleCourseInfo();

            CourseSchedule schedule = new CourseSchedule();
            schedule.setCourseId(courseInfo.getId());
            schedule.setTeacherId(teacherInfo == null ? null : teacherInfo.getId());
            schedule.setStartSection(startSection);
            schedule.setEndSection(endSection);
            schedule.setSemesterId(semesterId);
            schedule.setStartWeek(startWeek);
            schedule.setEndWeek(endWeek);
            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);
            schedule.setWeekType(weekType);
            schedule.setWeekdays(List.of(dayOfWeek));
            schedule.setLaboratoryId(laboratoryId);
            if (!success) {
                return new CourseSchedule.CourseScheduleHandle(schedule, success, errors.stream().map(ExcelParseError::toString).toList());
            }

            return new CourseSchedule.CourseScheduleHandle(schedule, true, new ArrayList<>());
        }

        private record SectionInfo(int section, LocalTime startTime, LocalTime endTime) {}

        private static final List<SectionInfo> COURSE_SCHEDULE = List.of(
                new SectionInfo(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
                new SectionInfo(2, LocalTime.of(8, 55), LocalTime.of(9, 40)),
                new SectionInfo(3, LocalTime.of(10, 0), LocalTime.of(10, 45)),
                new SectionInfo(4, LocalTime.of(10, 55), LocalTime.of(11, 40)),
                new SectionInfo(5, LocalTime.of(14, 10), LocalTime.of(14, 55)),
                new SectionInfo(6, LocalTime.of(15, 5), LocalTime.of(15, 50)),
                new SectionInfo(7, LocalTime.of(16, 0), LocalTime.of(16, 45)),
                new SectionInfo(8, LocalTime.of(16, 55), LocalTime.of(17, 40)),
                new SectionInfo(9, LocalTime.of(18, 40), LocalTime.of(19, 25)),
                new SectionInfo(10, LocalTime.of(19, 30), LocalTime.of(20, 15)),
                new SectionInfo(11, LocalTime.of(20, 20), LocalTime.of(21, 5))
        );

        private Handler handleWeekType() {
            if (!success) return this;
            if (CourseScheduleExcel.this.weekType == null){
                this.weekType = WeekType.Both;
                return this;
            }
            switch (CourseScheduleExcel.this.weekType) {
                case "单" -> this.weekType = WeekType.Single;
                case "双" -> this.weekType = WeekType.Double;
            }
            return this;
        }

        private Handler handleDayOfWeek() {
            if (!success) return this;
            dayOfWeek = columnIndex;
            return this;
        }

        private Handler handleTimeInfo() {
            if (!success) return this;

            if (timeInfo == null || timeInfo.isBlank()) {
                errors.add(new ExcelParseError(columnIndex, rowIndex, "时间信息不存在"));
                success = false;
                return this;
            }

            // 2. 匹配 HH:mm-HH:mm 时间格式，并反推最接近的节次
            Matcher timeMatcher = TIME_PATTERN.matcher(timeInfo);
            if (timeMatcher.find()) {
                LocalTime startTime = LocalTime.of(
                        Integer.parseInt(timeMatcher.group(1)),
                        Integer.parseInt(timeMatcher.group(2))
                );
                LocalTime endTime = LocalTime.of(
                        Integer.parseInt(timeMatcher.group(3)),
                        Integer.parseInt(timeMatcher.group(4))
                );
                this.startTime = startTime;
                this.endTime = endTime;
                this.startSection = findClosestSection(startTime, true);
                this.endSection = findClosestSection(endTime, false);
                return this;
            }

            // 1. 优先匹配 "X-Y节" 格式
            Matcher sectionMatcher = SECTION_PATTERN.matcher(timeInfo);
            if (sectionMatcher.find()) {
                int startSection = Integer.parseInt(sectionMatcher.group(1));
                int endSection = Integer.parseInt(sectionMatcher.group(2));
                this.startSection = startSection;
                this.endSection = endSection;

                LocalTime startTime = null;
                LocalTime endTime = null;
                for (SectionInfo info : COURSE_SCHEDULE) {
                    if (info.section() == startSection) {
                        startTime = info.startTime();
                    }
                    if (info.section() == endSection) {
                        endTime = info.endTime();
                    }
                }
                this.startTime = startTime;
                this.endTime = endTime;
                return this;
            }

            success = false;
            errors.add(new ExcelParseError(columnIndex, rowIndex, "未找到任何匹配的时间信息"));
            return this;
        }

        /**
         * 计算给定时间与课程时间表中最接近的节次
         *
         * @param time          给定时间
         * @param matchStartTime true-匹配开始时间 false-匹配结束时间
         */
        private Integer findClosestSection(LocalTime time, boolean matchStartTime) {
            long minMinutes = Long.MAX_VALUE;
            Integer closest = null;
            for (SectionInfo info : COURSE_SCHEDULE) {
                LocalTime target = matchStartTime ? info.startTime() : info.endTime();
                long minutes = Math.abs(Duration.between(time, target).toMinutes());
                if (minutes < minMinutes) {
                    minMinutes = minutes;
                    closest = info.section();
                }
            }
            return closest;
        }

        private Handler handleTeacherInfo() {
            if (!success) return this;

            if (teacher == null || teacher.isBlank()) {
                return this;
            }
            Teacher teacher$ = new Teacher();
            teacher$.setTeacherName(teacher);
            teacherMapper.insert(teacher$);
            teacherInfo = teacher$;
            return this;
        }

        private Handler handleCourseInfo() {
            if (!success) return this;

            if (name == null || name.isBlank()) {
                success = false;
                errors.add(new ExcelParseError(columnIndex, rowIndex, "课程信息不能为空"));
                return this;
            }

            Course course = new Course();
            course.setCourseName(name);
            course.setVolumn(0);
            courseMapper.insert(course);
            courseInfo = course;
            return this;
        }

    }
}
