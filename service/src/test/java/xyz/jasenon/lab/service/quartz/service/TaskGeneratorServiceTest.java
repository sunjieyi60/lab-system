package xyz.jasenon.lab.service.quartz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.ServiceApplication;
import xyz.jasenon.lab.service.quartz.mapper.ScheduleTaskMapper;
import xyz.jasenon.lab.service.quartz.mapper.TimeRuleMapper;
import xyz.jasenon.lab.service.quartz.model.CourseScheduleTaskGenerator;
import xyz.jasenon.lab.service.quartz.model.ScheduleTask;
import xyz.jasenon.lab.service.quartz.model.TimeRule;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskGeneratorService 单元测试
 * 使用 H2 内存数据库，@Transactional 自动回滚数据
 */
@SpringBootTest(classes = ServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class TaskGeneratorServiceTest {

    @Autowired
    private TaskGeneratorService taskGeneratorService;

    @Autowired
    private ScheduleTaskMapper scheduleTaskMapper;

    @Autowired
    private TimeRuleMapper timeRuleMapper;

    private CourseScheduleTaskGenerator createGenerator(List<Long> labIds, int earlyStart, int delayEnd) {
        CourseScheduleTaskGenerator generator = new CourseScheduleTaskGenerator();
        generator.setLaboratoryId(labIds);
        generator.setCron("0 0/5 * * * ?");
        generator.setEarlyStart(earlyStart);
        generator.setDelayEnd(delayEnd);
        generator.setEnable(true);
        return generator;
    }

    @Test
    void testGenerate_success() {
        R<Boolean> result = taskGeneratorService.generateScheduleTask(
                createGenerator(List.of(101L, 102L), 10, 5));

        assertTrue(result.getData());
        assertEquals("成功生成 3 个定时任务", result.getMsg());

        assertEquals(3, scheduleTaskMapper.selectList(new LambdaQueryWrapper<>()).size());
        assertEquals(3, timeRuleMapper.selectList(new LambdaQueryWrapper<>()).size());
    }

    @Test
    void testGenerate_specificLaboratory() {
        R<Boolean> result = taskGeneratorService.generateScheduleTask(
                createGenerator(List.of(102L), 0, 0));

        assertTrue(result.getData());
        assertEquals("成功生成 1 个定时任务", result.getMsg());

        List<ScheduleTask> tasks = scheduleTaskMapper.selectList(new LambdaQueryWrapper<>());
        assertEquals(1, tasks.size());
        assertTrue(tasks.get(0).getTaskName().contains("大学英语"));
    }

    @Test
    void testGenerate_emptyLaboratory() {
        R<Boolean> result = taskGeneratorService.generateScheduleTask(
                createGenerator(List.of(9999L), 0, 0));

        assertTrue(result.getData());
        assertEquals("没有找到课表数据", result.getMsg());

        assertEquals(0, scheduleTaskMapper.selectList(new LambdaQueryWrapper<>()).size());
    }

    @Test
    void testGenerate_timeRuleConversion() {
        taskGeneratorService.generateScheduleTask(createGenerator(List.of(102L), 10, 5));

        List<TimeRule> rules = timeRuleMapper.selectList(new LambdaQueryWrapper<>());
        assertEquals(1, rules.size());

        TimeRule rule = rules.get(0);
        assertNotNull(rule.getWeekdays(), "星期列表不应为空");
        assertEquals(List.of(2, 4), rule.getWeekdays());
        assertEquals(LocalTime.of(9, 50), rule.getStartTime(), "开始时间应提前10分钟");
        assertEquals(LocalTime.of(11, 45), rule.getEndTime(), "结束时间应延迟5分钟");
    }
}
