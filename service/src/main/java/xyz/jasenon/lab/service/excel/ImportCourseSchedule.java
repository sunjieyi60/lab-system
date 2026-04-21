package xyz.jasenon.lab.service.excel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.event.AnalysisEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.service.mapper.CourseMapper;
import xyz.jasenon.lab.service.mapper.CourseScheduleMapper;
import xyz.jasenon.lab.service.mapper.TeacherMapper;
import xyz.jasenon.lab.service.service.ICourseScheduleService;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@Service
@Slf4j
public class ImportCourseSchedule {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ICourseScheduleService courseScheduleService;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
     * 文本清洗映射表（key: 待替换字符/字符串, value: 替换后字符/字符串）。
     * 后续需要扩展时，直接在此表中追加键值对即可，无需修改 wash 方法。
     */
    private static final Map<String, String> WASH_MAP = new LinkedHashMap<>();

    static {
        WASH_MAP.put("：", ":");       // 全角冒号 → 半角冒号
        WASH_MAP.put("\r", "");        // 去除回车
        WASH_MAP.put("\n", "");        // 去除换行
    }

    /**
     * 基于映射表的文本清洗。
     * 遍历 WASH_MAP 中的每一组替换规则，对原始文本依次执行 replace。
     */
    private static String wash(String raw) {
        if (raw == null) return null;
        String result = raw;
        for (Map.Entry<String, String> entry : WASH_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public ImportResult importCourse(MultipartFile file, Long semesterId, Long laboratoryId) {
        TreeMap<Integer, List<CourseScheduleExcel>> columnMap = new TreeMap<>();
        List<ImportResult.ErrorItem> errorItems = new ArrayList<>();
        int[] okCount = {0};
        int[] failCount = {0};

        try {
            FesodSheet.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {

                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    Integer rowIdx = context.readRowHolder().getRowIndex();
                    data.forEach((index, value) -> {
                        if (value == null || value.isBlank()) return;

                        String washed = wash(value);

                        String[] values = washed.split(",");
                        for (String seg : values) {
                            String trimmedSeg = seg.trim();
                            if (!trimmedSeg.contains("<>")) continue;

                            CourseScheduleExcel excel = parse(trimmedSeg, index, rowIdx);
                            if (excel != null) {
                                columnMap.computeIfAbsent(index, k -> new ArrayList<>()).add(excel);
                            } else {
                                errorItems.add(ImportResult.ErrorItem.builder()
                                        .rowIndex(rowIdx)
                                        .columnIndex(index)
                                        .rawContent(value)
                                        .reason("单元格内容格式不符合要求，期望格式：课程<>时间<>地点<>教师")
                                        .build());
                                failCount[0]++;
                            }
                        }

                    });

                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {

                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<CourseSchedule> schedules = courseScheduleService.list(
                new LambdaQueryWrapper<CourseSchedule>().eq(CourseSchedule::getSemesterId, semesterId)
        );
        // 转换为 CourseSchedule 并持久化，每条独立事务，失败自动回滚已插入的 course / teacher
        for (List<CourseScheduleExcel> list : columnMap.values()) {
            for (CourseScheduleExcel excel : list) {
                CourseSchedule.CourseScheduleHandle handle = processSingle(excel, semesterId, laboratoryId, schedules);
                if (Boolean.TRUE.equals(handle.getSuccess())) {
                    okCount[0]++;
                } else {
                    failCount[0]++;
                    if (handle.getErrors() != null && !handle.getErrors().isEmpty()) {
                        for (String err : handle.getErrors()) {
                            errorItems.add(ImportResult.ErrorItem.builder()
                                    .rowIndex(excel.getRowIndex())
                                    .columnIndex(excel.getColumnIndex())
                                    .rawContent(excel.getRawContent())
                                    .reason(err)
                                    .build());
                        }
                    }
                }
            }
        }

        return ImportResult.builder()
                .ok(okCount[0])
                .fail(failCount[0])
                .errors(errorItems)
                .build();
    }

    public CourseSchedule.CourseScheduleHandle processSingle(CourseScheduleExcel excel, Long semesterId, Long laboratoryId, List<CourseSchedule> olds) {
        final CourseSchedule.CourseScheduleHandle[] holder = new CourseSchedule.CourseScheduleHandle[1];
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                CourseSchedule.CourseScheduleHandle handle = excel.handle(teacherMapper, courseMapper)
                        .convert(semesterId, laboratoryId);
                if (Boolean.TRUE.equals(handle.getSuccess())) {
//                    courseScheduleMapper.insert(handle.getSchedule());
                    boolean conflict = courseScheduleService.checkConflect(handle.getSchedule(), olds);
                    if (conflict){
                        handle.setSuccess(false);
                        handle.getErrors().add(String.format("%s 存在时间冲突 插入失败", handle.getSchedule().toString()));
                        status.setRollbackOnly();
                    }
                    courseScheduleService.save(handle.getSchedule());
                    olds.add(handle.getSchedule());
                } else {
                    status.setRollbackOnly();
                }
                holder[0] = handle;
            }
        });
        return holder[0];
    }

    private CourseScheduleExcel parse(String raw, Integer colIdx, Integer rowIdx) {
        Matcher main = COURSE_PATTERN.matcher(raw);
        if (!main.find()) {
            log.warn("[解析失败] 行{}列{}: 基础格式不匹配 -> {}",
                    rowIdx, colIdx,
                    raw.length() > 40 ? raw.substring(0, 40) + "..." : raw);
            return null;
        }

        String name = main.group(1).trim();
        String timeField = main.group(2).trim();
        String location = main.group(3).trim();
        String teacher = main.group(4).trim();

        Matcher timeMatcher = TIME_PATTERN.matcher(timeField);
        if (!timeMatcher.find()) {
            log.warn("[解析失败] 行{}列{}: 时间格式异常（期望X-Y周[内容]） -> {}",
                    rowIdx, colIdx, timeField);
            return null;
        }

        try {
            Integer startWeek = Integer.parseInt(timeMatcher.group(1));
            Integer endWeek = Integer.parseInt(timeMatcher.group(2));
            String weekType = timeMatcher.group(3);  // 可能为null
            String timeDetail = timeMatcher.group(4);  // []内的原始内容，原样保留

            CourseScheduleExcel excel = CourseScheduleExcel.builder()
                    .startWeek(startWeek)
                    .endWeek(endWeek)
                    .timeInfo(timeDetail)
                    .weekType(weekType)
                    .name(name)
                    .teacher(teacher)
                    .columnIndex(colIdx)
                    .rowIndex(rowIdx)
                    .rawContent(raw)
                    .build();
            return excel;
        } catch (NumberFormatException e) {
            log.warn("[解析失败] 行{}列{}: 周次数字格式异常 -> {}", rowIdx, colIdx, timeField);
            return null;
        }
    }
}
