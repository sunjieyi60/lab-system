package xyz.jasenon.lab.service;

import com.alibaba.fastjson2.JSON;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.enums.CellExtraTypeEnum;
import org.apache.fesod.sheet.event.AnalysisEventListener;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JwExcelCourseParser {

    @Data
    @Builder
    public static class Course {
        private String rawText;        // 原始完整字符串（溯源用）
        private String name;           // 课程名
        private String location;       // 地点
        private String teacher;        // 教师

        // 时间解析结果
        private Integer startWeek;     // 起始周（数字）
        private Integer endWeek;       // 结束周（数字）
        private String weekType;       // 单双周标记："单"、"双" 或 null
        private String timeDetail;     // []内的原始内容（如 "9-11节" 或 "上课时间：18l:00-21:00"）

        private Integer startSection;
        private Integer endSection;

        private LocalTime startTime;
        private LocalTime endTime;

        // 处理异常信息
        private List<String> errors;
        private boolean hasError;

        // 位置信息
        private Integer columnIndex;
        private Long rowIndex;

        public Handler handler() {
            return new Handler(this);
        }

        public CourseSchedule convert() {
            CourseSchedule schedule = new CourseSchedule();
            schedule.setStartWeek(this.startWeek);
            schedule.setEndWeek(this.endWeek);
            if (this.weekType != null) {
                schedule.setWeekType("单".equals(this.weekType) ? WeekType.Single : WeekType.Double);
            } else {
                schedule.setWeekType(WeekType.Both);
            }
            schedule.setStartSection(this.startSection);
            schedule.setEndSection(this.endSection);
            schedule.setStartTime(this.startTime);
            schedule.setEndTime(this.endTime);
            schedule.setMark(this.rawText);
            return schedule;
        }

        @Data
        private static class Handler {
            private final Course course;
            private final List<String> errors = new ArrayList<>();
            private boolean hasError = false;

            private static final Pattern SECTION_PATTERN = Pattern.compile("(\\d+)-(\\d+)节");
            private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+):(\\d+)-(\\d+):(\\d+)");

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

            public Handler handleTimeDetail() {
                String timeDetail = course.getTimeDetail();
                if (timeDetail == null || timeDetail.isBlank()) {
                    return this;
                }

                // 1. 优先匹配 "X-Y节" 格式
                Matcher sectionMatcher = SECTION_PATTERN.matcher(timeDetail);
                if (sectionMatcher.find()) {
                    int startSection = Integer.parseInt(sectionMatcher.group(1));
                    int endSection = Integer.parseInt(sectionMatcher.group(2));
                    course.setStartSection(startSection);
                    course.setEndSection(endSection);

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
                    course.setStartTime(startTime);
                    course.setEndTime(endTime);
                    return this;
                }

                // 2. 匹配 HH:mm-HH:mm 时间格式，并反推最接近的节次
                Matcher timeMatcher = TIME_PATTERN.matcher(timeDetail);
                if (timeMatcher.find()) {
                    LocalTime startTime = LocalTime.of(
                            Integer.parseInt(timeMatcher.group(1)),
                            Integer.parseInt(timeMatcher.group(2))
                    );
                    LocalTime endTime = LocalTime.of(
                            Integer.parseInt(timeMatcher.group(3)),
                            Integer.parseInt(timeMatcher.group(4))
                    );
                    course.setStartTime(startTime);
                    course.setEndTime(endTime);
                    course.setStartSection(findClosestSection(startTime, true));
                    course.setEndSection(findClosestSection(endTime, false));
                    return this;
                }

                // 3. 两类均不匹配，记录错误
                this.hasError = true;
                String errorMsg = String.format(
                        "timeDetail '%s' 无法匹配节次格式(X-Y节)或时间格式(HH:mm-HH:mm)", timeDetail);
                this.errors.add(errorMsg);
                this.course.setHasError(true);
                if (this.course.getErrors() == null) {
                    this.course.setErrors(new ArrayList<>());
                }
                this.course.getErrors().add(errorMsg);
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

            /**
             * 完成 timeDetail 二次处理后，转换为内部 CourseSchedule 实体
             */
            public CourseSchedule handle() {
                this.handleTimeDetail();
                return this.course.convert();
            }
        }
    }

    // 主正则：提取课程<>时间<>地点<>教师
    private static final Pattern COURSE_PATTERN = Pattern.compile(
            "([^<>]+)<>([^<>]+)<>([^<>]+)<>([^,]+)"
    );

    // 时间正则：提取周次范围、可选单双周标记、[]内原始内容
    // 组1:起始周, 组2:结束周, 组3:单双周(可能null), 组4:[]内的原始字符串
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d+)-(\\d+)周" +              // 周次范围 1-16周
                    "(?:\\((单|双)\\))?" +            // 可选：单双周标记 (单)或(双)
                    "\\[(.*?)]"                      // []内的任意内容（非贪婪匹配）
    );

    /**
     * 全角符号清洗：将全角数字、字母、空格、冒号、逗号等转为半角
     */
    private static String wash(String raw) {
        if (raw == null) return null;
        StringBuilder sb = new StringBuilder(raw.length());
        for (char c : raw.toCharArray()) {
            if (c == '\u3000') {
                sb.append(' ');
            } else if (c >= '\uFF10' && c <= '\uFF19') {
                sb.append((char) (c - '\uFF10' + '0'));
            } else if (c >= '\uFF21' && c <= '\uFF3A') {
                sb.append((char) (c - '\uFF21' + 'A'));
            } else if (c >= '\uFF41' && c <= '\uFF5A') {
                sb.append((char) (c - '\uFF41' + 'a'));
            } else if (c == '\uFF1A') {
                sb.append(':');
            } else if (c == '\uFF0C') {
                sb.append(',');
            } else if (c == '\uFF1C') {
                sb.append('<');
            } else if (c == '\uFF1E') {
                sb.append('>');
            } else if (c == '\u3010') {
                sb.append('[');
            } else if (c == '\u3011') {
                sb.append(']');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Course parseCourse(String raw, int colIdx, Long rowIdx) {
        // 1. 匹配四大字段
        Matcher mainMatcher = COURSE_PATTERN.matcher(raw);
        if (!mainMatcher.find()) {
            log.warn("[解析失败] 行{}列{}: 基础格式不匹配 -> {}",
                    rowIdx, colIdx,
                    raw.length() > 40 ? raw.substring(0, 40) + "..." : raw);
            return null;
        }

        String name = mainMatcher.group(1).trim();
        String timeField = mainMatcher.group(2).trim();
        String location = mainMatcher.group(3).trim();
        String teacher = mainMatcher.group(4).trim();

        // 2. 匹配时间字段：提取周次和[]内容
        Matcher timeMatcher = TIME_PATTERN.matcher(timeField);
        if (!timeMatcher.find()) {
            log.warn("[解析失败] 行{}列{}: 时间格式异常（期望X-Y周[内容]） -> {}",
                    rowIdx, colIdx, timeField);
            return null;
        }

        try {
            int startWeek = Integer.parseInt(timeMatcher.group(1));
            int endWeek = Integer.parseInt(timeMatcher.group(2));
            String weekType = timeMatcher.group(3);  // 可能为null
            String timeDetail = timeMatcher.group(4);  // []内的原始内容，原样保留

            Course course = Course.builder()
                    .rawText(raw)
                    .columnIndex(colIdx)
                    .rowIndex(rowIdx)
                    .name(name)
                    .location(location)
                    .teacher(teacher)
                    .startWeek(startWeek)
                    .endWeek(endWeek)
                    .weekType(weekType)
                    .timeDetail(timeDetail)
                    .build();

            // 3. 二次处理 timeDetail，明确起止节次与起止时间
            course.handler().handleTimeDetail();
            return course;

        } catch (NumberFormatException e) {
            log.error("[解析失败] 行{}列{}: 周次数字解析异常 -> {}", rowIdx, colIdx, timeField);
            return null;
        }
    }

    public static void main(String[] args) {
        String filePath = "/Users/Zhuanz/Documents/206.xls";
        TreeMap<Integer, List<Course>> columnMap = new TreeMap<>();

        FesodSheet.read(filePath, new AnalysisEventListener<Map<Integer, String>>() {
                    @Override
                    public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                        Long rowIdx = Long.valueOf(context.readRowHolder().getRowIndex());

                        rowData.forEach((colIdx, value) -> {
                            if (value == null || value.isBlank()) return;

                            // 全角符号清洗
                            String washed = wash(value);

                            // 按逗号分割同一单元格内的多门课程
                            String[] segments = washed.split(",");
                            for (String seg : segments) {
                                seg = seg.trim();
                                if (!seg.contains("<>")) continue;

                                Course c = parseCourse(seg, colIdx, rowIdx);
                                if (c != null) {
                                    columnMap.computeIfAbsent(colIdx, k -> new ArrayList<>()).add(c);
                                }
                            }
                        });
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {
                        log.info("========================================");
                        log.info("✅ 解析完成，共 {} 列包含课程数据", columnMap.size());

                        List<Course> allCourses = new ArrayList<>();

                        columnMap.forEach((colIdx, list) -> {
                            log.info("----------------------------------------");
                            log.info("📍 第 {} 列（Excel第{}列），共 {} 条", colIdx, colIdx + 1, list.size());

                            list.forEach(c -> {
                                // 简洁输出：展示提取的结构
                                String weekInfo = String.format("%d-%d周%s",
                                        c.getStartWeek(), c.getEndWeek(),
                                        c.getWeekType() != null ? "(" + c.getWeekType() + ")" : "");

                                log.info("  课程: {}", c.getName());
                                log.info("  时间域: {} | 内容: [{}] | 节次: {}-{}节 | 时间: {}~{}",
                                        weekInfo, c.getTimeDetail(),
                                        c.getStartSection(), c.getEndSection(),
                                        c.getStartTime(), c.getEndTime());
                                log.info("  地点: {} | 教师: {}", c.getLocation(), c.getTeacher());
                            });

                            allCourses.addAll(list);
                        });

                        log.info("========================================");
                        log.info("📊 总计 {} 门课程", allCourses.size());

                        // 输出完整JSON，包含二次处理后的节次和时间
                        log.info("📝 Course JSON输出: {}", JSON.toJSONString(allCourses));

                        // 演示：转换为内部 CourseSchedule
                        List<CourseSchedule> schedules = allCourses.stream()
                                .map(Course::convert)
                                .toList();
                        log.info("📝 CourseSchedule JSON输出: {}", JSON.toJSONString(schedules));
                    }
                })
                .extraRead(CellExtraTypeEnum.MERGE)
                .sheet()
                .doRead();
    }

}
