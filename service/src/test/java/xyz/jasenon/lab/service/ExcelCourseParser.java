package xyz.jasenon.lab.service;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.enums.CellExtraTypeEnum;
import org.apache.fesod.sheet.event.AnalysisEventListener;
import org.apache.poi.ss.usermodel.DateUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel课程表解析器
 * 支持格式：课程名[班级]周次星期节次教师列表[工号] 可选时间后缀HH:mm-HH:mm
 * 自动清洗Excel中的全角空格、不间断空格、换行符等不可控字符
 */
@Slf4j
public class ExcelCourseParser {

    // 按列存储原始数据：key=列号(0-based)，value=该列所有单元格文本
    private final TreeMap<Integer, List<String>> columnMap = new TreeMap<>();

    // 解析结果存储
    private final List<Course> parsedCourses = new ArrayList<>();

    /**
     * 课程实体
     */
    @Data
    public static class Course {
        private String name;           // 课程名，如"软件工程(实验)"
        private String classId;        // 班级代码，如"01"
        private String weekRange;      // 周次，如"7-12周"、"1-16周(单)"
        private String dayOfWeek;      // 星期，如"星期一"
        private String section;        // 节次，如"第5-8节"
        private List<Teacher> teachers; // 教师列表
        private String timeRange;      // 可选：实际时间范围，如"14:30-16:00"

        @Override
        public String toString() {
            String teacherStr = teachers.stream()
                    .map(Teacher::toString)
                    .collect(Collectors.joining(","));
            return timeRange != null
                    ? String.format("%s[%s] %s %s %s %s 教师:%s",
                    name, classId, weekRange, dayOfWeek, section, timeRange, teacherStr)
                    : String.format("%s[%s] %s %s %s 教师:%s",
                    name, classId, weekRange, dayOfWeek, section, teacherStr);
        }
    }

    /**
     * 教师实体
     */
    @Data
    public static class Teacher {
        private String name;   // 姓名
        private String id;     // 工号，可能为null（如"杨金明"无工号）

        public Teacher(String name, String id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return id != null ? name + "[" + id + "]" : name;
        }
    }

    /**
     * 激进清洗：移除所有Unicode空白字符，修复时间格式
     * 处理：半角空格、全角空格(\u3000)、不间断空格(\u00A0)、换行(\n)、制表符(\t)等
     */
    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String cleaned = raw
                // 先修复时间格式内部的空格（如"14: 30" -> "14:30"）
                .replaceAll("(\\d{1,2})[\\s:]+(\\d{2})", "$1:$2")
                // 删除所有Unicode空白字符
                .replaceAll("[\\s\\u00A0\\u3000\\u2000-\\u200F]+", "")
                .trim();

        return cleaned;
    }

    /**
     * 主正则：支持可选时间后缀
     * 教师部分使用非贪婪匹配(.+?)，防止吞噬后面的时间
     */
    private static final Pattern MAIN_PATTERN = Pattern.compile(
            "^(.*?)" +                              // 1. 课程名（非贪婪）
                    "\\[(\\d+)\\]" +                         // 2. 班级代码 [01]
                    "((?:\\d+-)?\\d+周(?:\\([单双]\\))?)" +    // 3. 周次：7-12周 / 1-16周(单) / 2-2周
                    "(星期[一二三四五六日])" +                 // 4. 星期
                    "\\((第[\\d-]+节)\\)" +                  // 5. 节次：(第5-8节)
                    "(.+?)" +                               // 6. 教师部分（非贪婪匹配！关键）
                    "(?:(\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}))?" + // 7. 可选时间后缀：14:30-16:00
                    "$"
    );

    /**
     * 教师解析正则：支持 姓名[工号] 或 纯姓名
     * 姓名限制2-4个汉字，工号4-10位数字
     */
    private static final Pattern TEACHER_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5]{2,4})(?:\\[(\\d{4,10})\\])?"
    );

    /**
     * 解析单条课程字符串
     */
    public Optional<Course> parse(String raw) {
        String clean = sanitize(raw);
        if (clean.isEmpty()) return Optional.empty();

        Matcher m = MAIN_PATTERN.matcher(clean);
        if (!m.find()) {
            log.warn("正则匹配失败，原始内容: [{}], 清洗后: [{}]", raw, clean);
            return Optional.empty();
        }

        Course course = new Course();
        course.setName(m.group(1));
        course.setClassId(m.group(2));
        course.setWeekRange(m.group(3));
        course.setDayOfWeek(m.group(4));
        course.setSection(m.group(5));
        course.setTimeRange(m.group(7)); // 可能为null

        // 解析教师列表（可能逗号分隔）
        String teacherPart = m.group(6);
        List<Teacher> teachers = new ArrayList<>();

        // 按逗号分隔多个教师
        String[] teacherSegments = teacherPart.split(",");
        for (String segment : teacherSegments) {
            String t = segment.trim();
            if (t.isEmpty()) continue;

            Matcher tm = TEACHER_PATTERN.matcher(t);
            if (tm.find()) {
                teachers.add(new Teacher(tm.group(1), tm.group(2)));
            } else {
                log.warn("教师解析失败: [{}] (来自原始字符串: {})", t, raw);
            }
        }

        course.setTeachers(teachers);
        return Optional.of(course);
    }

    /**
     * 执行Excel读取和解析
     */
    public List<Course> parseExcel(String filePath) {
        columnMap.clear();
        parsedCourses.clear();

        log.info("开始解析Excel文件: {}", filePath);

        FesodSheet.read(filePath, new AnalysisEventListener<Map<Integer, String>>() {
                    @Override
                    public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                        int rowIndex = context.readRowHolder().getRowIndex();
                        log.debug("读取第{}行，{}列有数据", rowIndex, rowData.size());

                        rowData.forEach((colIndex, value) -> {
                            if (value != null && !value.isBlank()) {
                                // 按双换行符分割同一单元格内的多门课程
                                String[] courses = value.split("\\n\\n");
                                for (String courseStr : courses) {
                                    if (courseStr != null && !courseStr.isBlank()) {
                                        columnMap.computeIfAbsent(colIndex, k -> new ArrayList<>())
                                                .add(courseStr.trim());
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {
                        log.info("Excel读取完成，共{}列数据待解析", columnMap.size());

                        columnMap.forEach((colIndex, values) -> {
                            log.debug("解析第{}列，共{}条原始数据", colIndex, values.size());

                            values.forEach(raw -> {
                                parse(raw).ifPresent(course -> {
                                    parsedCourses.add(course);
                                    log.debug("解析成功: {}", course);
                                });
                            });
                        });

                        log.info("解析完成，共成功解析{}条课程记录", parsedCourses.size());
                    }
                })
                .extraRead(CellExtraTypeEnum.MERGE) // 读取合并单元格信息
                .sheet()
                .doRead();

        return parsedCourses;
    }

    /**
     * 获取解析结果（JSON格式）
     */
    public String getResultAsJson() {
        return JSON.toJSONString(parsedCourses);
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        String path = "/Users/Zhuanz/Documents/202.xlsx";

        ExcelCourseParser parser = new ExcelCourseParser();

        // 执行解析
        List<Course> courses = parser.parseExcel(path);

        // 控制台输出
        System.out.println("=== 解析结果 ===");
        courses.forEach(System.out::println);

        // JSON输出
        System.out.println("\n=== JSON格式 ===");
        System.out.println(parser.getResultAsJson());

        // 测试单条解析（用于调试）
        System.out.println("\n=== 单条测试 ===");
        String[] testCases = {
                "软件工程(实验)[01]7-12周星期一(第5-8节)李伟[2013019]",
                "医学图像处理(实验)[02]14-16周星期一(第5-8节)刘李漫[2013087],唐奇伶[3099810]",
                "药物化学实验[02]2-2周星期四(第5-8节)杨金明 14:30-16:00",  // 无工号+时间
                "人工智能与Python程序设计(实验)[01]1-16周星期二(第5-6节)李芸[3029907] 09:00-10:30"
        };

        for (String test : testCases) {
            parser.parse(test).ifPresentOrElse(
                    c -> System.out.println("✅ " + c),
                    () -> System.out.println("❌ 解析失败: " + test)
            );
        }
    }
}