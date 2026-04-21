package xyz.jasenon.lab.service.quartz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.entity.class_time_table.Course;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.mapper.CourseMapper;
import xyz.jasenon.lab.service.mapper.CourseScheduleMapper;
import xyz.jasenon.lab.service.mapper.SemesterMapper;
import xyz.jasenon.lab.service.quartz.mapper.*;
import xyz.jasenon.lab.service.quartz.model.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TaskGeneratorService 单元测试
 * <p>
 * 核心验证点：
 * 1. 基础课表生成逻辑（任务 + 时间规则）
 * 2. 智能控制模板的深度复制（ID 重分配 + 关联映射）
 */
@ExtendWith(MockitoExtension.class)
class TaskGeneratorServiceTest {

    @InjectMocks
    private TaskGeneratorService taskGeneratorService;

    @Mock
    private ScheduleTaskMapper scheduleTaskMapper;
    @Mock
    private TimeRuleMapper timeRuleMapper;
    @Mock
    private CourseScheduleMapper courseScheduleMapper;
    @Mock
    private SemesterMapper semesterMapper;
    @Mock
    private CourseMapper courseMapper;

    // 智能控制模板相关 Mapper
    @Mock
    private ConditionGroupMapper conditionGroupMapper;
    @Mock
    private ConditionMapper conditionMapper;
    @Mock
    private ActionGroupMapper actionGroupMapper;
    @Mock
    private ActionMapper actionMapper;
    @Mock
    private DataMapper dataMapper;

    /**
     * 场景1：不携带智能控制模板，仅生成基础任务 + 时间规则
     */
    @Test
    void generateScheduleTask_withoutSmartControl_shouldGenerateBasicTasks() {
        // ---------- 准备数据 ----------
        CourseScheduleTaskGenerator target = new CourseScheduleTaskGenerator();
        target.setLaboratoryId(Collections.singletonList(1L));
        target.setSemesterId(100L);
        target.setCron("0 0/5 * * * ?");
        target.setEarlyStart(5);
        target.setDelayEnd(10);
        target.setEnable(true);
        // 不设置 conditionGroups / actionGroups / dataGroup

        // 构造 2 条课表记录
        CourseSchedule cs1 = createCourseSchedule(1L, 100L, 1L, 1, 16, 8, 0);
        CourseSchedule cs2 = createCourseSchedule(2L, 100L, 2L, 3, 16, 14, 0);
        List<CourseSchedule> courseSchedules = Arrays.asList(cs1, cs2);

        Semester semester = new Semester();
        semester.setStartDate(LocalDate.of(2026, 2, 1));
        semester.setEndDate(LocalDate.of(2026, 7, 1));

        Course course1 = new Course();
        course1.setCourseName("Java程序设计");
        Course course2 = new Course();
        course2.setCourseName("数据结构");

        // ---------- Mock 行为 ----------
        when(courseScheduleMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(courseSchedules);
        when(semesterMapper.selectById(100L)).thenReturn(semester);
        when(courseMapper.selectById(1L)).thenReturn(course1);
        when(courseMapper.selectById(2L)).thenReturn(course2);

        // ---------- 执行 ----------
        R<Boolean> result = taskGeneratorService.generateScheduleTask(target);

        // ---------- 验证 ----------
        assertTrue(result.isOk());
        assertTrue(result.getData());
        assertTrue(result.getMsg().contains("2"));

        // 任务主体 + 时间规则各插入 2 次
        verify(scheduleTaskMapper, times(2)).insert(any(ScheduleTask.class));
        verify(timeRuleMapper, times(2)).insert(any(TimeRule.class));

        // 智能控制相关 Mapper 不应被调用
        verifyNoInteractions(conditionGroupMapper, conditionMapper,
                actionGroupMapper, actionMapper, dataMapper);
    }

    /**
     * 场景2：携带完整智能控制模板，验证深度复制 + ID 重分配 + conditionGroupId 关联映射
     */
    @Test
    void generateScheduleTask_withSmartControl_shouldDeepCopyAndRemapIds() {
        // ---------- 准备数据 ----------
        String templateCgId1 = "template-cg-001";
        String templateCgId2 = "template-cg-002";
        String templateAgId1 = "template-ag-001";
        String templateAgId2 = "template-ag-002";

        // 条件组模板（带旧ID，用于动作组关联）
        ConditionGroup cg1 = new ConditionGroup();
        cg1.setId(templateCgId1);
        cg1.setType(ConditionGroup.ConditionGroupType.ALL);
        Condition cond1 = new Condition();
        cond1.setExpr("#{100}.roomTemperature > 26");
        cond1.setDesc("温度过高");
        cg1.setConditions(Collections.singletonList(cond1));

        ConditionGroup cg2 = new ConditionGroup();
        cg2.setId(templateCgId2);
        cg2.setType(ConditionGroup.ConditionGroupType.ANY);
        Condition cond2 = new Condition();
        cond2.setExpr("#{101}.isOpen == false");
        cond2.setDesc("设备关闭");
        cg2.setConditions(Collections.singletonList(cond2));

        // 数据源模板（带ID，用于条件表达式引用验证）
        Data data1 = new Data();
        data1.setId("100");  // 对应 cond1 表达式 #{100}
        data1.setDeviceId(200L);
        data1.setDeviceType(DeviceType.Sensor);

        Data data2 = new Data();
        data2.setId("101");  // 对应 cond2 表达式 #{101}
        data2.setDeviceId(201L);
        data2.setDeviceType(DeviceType.AirCondition);

        // 动作组模板（conditionGroupId 指向模板中的条件组ID）
        ActionGroup ag1 = new ActionGroup();
        ag1.setId(templateAgId1);
        ag1.setConditionGroupId(templateCgId1); // 关联 cg1
        Action action1 = new Action();
        action1.setDeviceType(DeviceType.AirCondition);
        action1.setDeviceId(101L);
        action1.setCommandLine(CommandLine.OPEN_AIR_CONDITION_RS485);
        action1.setArgs(new Integer[]{35, 1});
        ag1.setActions(Collections.singletonList(action1));

        ActionGroup ag2 = new ActionGroup();
        ag2.setId(templateAgId2);
        ag2.setConditionGroupId(templateCgId2); // 关联 cg2
        Action action2 = new Action();
        action2.setDeviceType(DeviceType.Light);
        action2.setDeviceId(102L);
        action2.setCommandLine(CommandLine.OPEN_LIGHT);
        action2.setArgs(new Integer[]{41, 1});
        ag2.setActions(Collections.singletonList(action2));

        CourseScheduleTaskGenerator target = new CourseScheduleTaskGenerator();
        target.setLaboratoryId(Collections.singletonList(1L));
        target.setSemesterId(100L);
        target.setCron("0 0/5 * * * ?");
        target.setEarlyStart(7);
        target.setDelayEnd(7);
        target.setEnable(true);
        target.setConditionGroups(Arrays.asList(cg1, cg2));
        target.setActionGroups(Arrays.asList(ag1, ag2));
        target.setDataGroup(Arrays.asList(data1, data2));

        // 构造 1 条课表记录（确保只生成 1 个任务，便于精确验证）
        CourseSchedule cs = createCourseSchedule(1L, 100L, 1L, 1, 16, 8, 0);
        List<CourseSchedule> courseSchedules = Collections.singletonList(cs);

        Semester semester = new Semester();
        semester.setStartDate(LocalDate.of(2026, 2, 1));
        semester.setEndDate(LocalDate.of(2026, 7, 1));

        Course course = new Course();
        course.setCourseName("Java程序设计");

        // ---------- Mock 行为 ----------
        when(courseScheduleMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(courseSchedules);
        when(semesterMapper.selectById(100L)).thenReturn(semester);
        when(courseMapper.selectById(1L)).thenReturn(course);

        // ---------- 执行 ----------
        R<Boolean> result = taskGeneratorService.generateScheduleTask(target);

        // ---------- 验证基础逻辑 ----------
        assertTrue(result.isOk());
        verify(scheduleTaskMapper, times(1)).insert(any(ScheduleTask.class));
        verify(timeRuleMapper, times(1)).insert(any(TimeRule.class));

        // ---------- 验证条件组深度复制 ----------
        ArgumentCaptor<ConditionGroup> cgCaptor = ArgumentCaptor.forClass(ConditionGroup.class);
        verify(conditionGroupMapper, times(2)).insert(cgCaptor.capture());
        List<ConditionGroup> insertedCgList = cgCaptor.getAllValues();

        // 新 ID 不能为 null，也不能等于模板 ID
        String newCgId1 = insertedCgList.get(0).getId();
        String newCgId2 = insertedCgList.get(1).getId();
        assertNotNull(newCgId1);
        assertNotNull(newCgId2);
        assertNotEquals(templateCgId1, newCgId1);
        assertNotEquals(templateCgId2, newCgId2);

        // scheduleTaskId 必须指向新任务
        assertNotNull(insertedCgList.get(0).getScheduleTaskId());
        assertNotNull(insertedCgList.get(1).getScheduleTaskId());
        assertEquals(insertedCgList.get(0).getScheduleTaskId(), insertedCgList.get(1).getScheduleTaskId());

        // ---------- 验证条件深度复制 ----------
        ArgumentCaptor<Condition> condCaptor = ArgumentCaptor.forClass(Condition.class);
        verify(conditionMapper, times(2)).insert(condCaptor.capture());
        List<Condition> insertedCondList = condCaptor.getAllValues();

        assertNotNull(insertedCondList.get(0).getId());
        assertNotEquals(cond1.getId(), insertedCondList.get(0).getId());
        assertEquals(newCgId1, insertedCondList.get(0).getConditionGroupId());
        assertNotNull(insertedCondList.get(0).getScheduleTaskId());

        assertNotNull(insertedCondList.get(1).getId());
        assertEquals(newCgId2, insertedCondList.get(1).getConditionGroupId());

        // ---------- 验证数据源深度复制 ----------
        ArgumentCaptor<Data> dataCaptor = ArgumentCaptor.forClass(Data.class);
        verify(dataMapper, times(2)).insert(dataCaptor.capture());
        List<Data> insertedDataList = dataCaptor.getAllValues();

        String newDataId1 = insertedDataList.get(0).getId();
        String newDataId2 = insertedDataList.get(1).getId();
        assertNotNull(newDataId1);
        assertNotNull(newDataId2);
        assertNotEquals("100", newDataId1);
        assertNotEquals("101", newDataId2);
        assertEquals(DeviceType.Sensor, insertedDataList.get(0).getDeviceType());
        assertEquals(200L, insertedDataList.get(0).getDeviceId());
        assertEquals(DeviceType.AirCondition, insertedDataList.get(1).getDeviceType());
        assertEquals(201L, insertedDataList.get(1).getDeviceId());
        assertNotNull(insertedDataList.get(0).getScheduleTaskId());

        // ---------- 验证条件表达式中的 dataId 已被替换 ----------
        // cond1 原始 expr = #{100}.roomTemperature > 26，应当被替换为 #{newDataId1}...
        String replacedExpr1 = insertedCondList.get(0).getExpr();
        assertFalse(replacedExpr1.contains("#{100}"), "表达式中的旧 dataId 应当被替换");
        assertTrue(replacedExpr1.contains("#{" + newDataId1 + "}"), "表达式应当包含新的 dataId");
        assertTrue(replacedExpr1.contains(".roomTemperature > 26"));

        String replacedExpr2 = insertedCondList.get(1).getExpr();
        assertFalse(replacedExpr2.contains("#{101}"), "表达式中的旧 dataId 应当被替换");
        assertTrue(replacedExpr2.contains("#{" + newDataId2 + "}"), "表达式应当包含新的 dataId");
        assertTrue(replacedExpr2.contains(".isOpen == false"));

        // ---------- 验证动作组深度复制 + conditionGroupId 关联映射 ----------
        ArgumentCaptor<ActionGroup> agCaptor = ArgumentCaptor.forClass(ActionGroup.class);
        verify(actionGroupMapper, times(2)).insert(agCaptor.capture());
        List<ActionGroup> insertedAgList = agCaptor.getAllValues();

        String newAgId1 = insertedAgList.get(0).getId();
        String newAgId2 = insertedAgList.get(1).getId();
        assertNotNull(newAgId1);
        assertNotNull(newAgId2);
        assertNotEquals(templateAgId1, newAgId1);
        assertNotEquals(templateAgId2, newAgId2);

        // 关键验证：conditionGroupId 必须被正确映射到新生成的条件组 ID
        // ag1 的 conditionGroupId 应该等于 newCgId1（而不是 templateCgId1）
        assertEquals(newCgId1, insertedAgList.get(0).getConditionGroupId());
        // ag2 的 conditionGroupId 应该等于 newCgId2（而不是 templateCgId2）
        assertEquals(newCgId2, insertedAgList.get(1).getConditionGroupId());

        assertNotNull(insertedAgList.get(0).getScheduleTaskId());
        assertNotNull(insertedAgList.get(1).getScheduleTaskId());

        // ---------- 验证动作深度复制 ----------
        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        verify(actionMapper, times(2)).insert(actionCaptor.capture());
        List<Action> insertedActionList = actionCaptor.getAllValues();

        assertNotNull(insertedActionList.get(0).getId());
        assertEquals(newAgId1, insertedActionList.get(0).getActionGroupId());
        assertNotNull(insertedActionList.get(0).getScheduleTaskId());

        assertNotNull(insertedActionList.get(1).getId());
        assertEquals(newAgId2, insertedActionList.get(1).getActionGroupId());
    }

    /**
     * 场景3：课表为空时应返回友好提示，不执行任何插入
     */
    @Test
    void generateScheduleTask_emptyCourseSchedule_shouldReturnFriendlyMessage() {
        CourseScheduleTaskGenerator target = new CourseScheduleTaskGenerator();
        target.setLaboratoryId(Collections.singletonList(999L));
        target.setSemesterId(100L);

        when(courseScheduleMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        R<Boolean> result = taskGeneratorService.generateScheduleTask(target);

        assertTrue(result.isOk());
        assertTrue(result.getMsg().contains("没有找到课表数据"));
        verifyNoInteractions(scheduleTaskMapper, timeRuleMapper, semesterMapper,
                conditionGroupMapper, conditionMapper, actionGroupMapper, actionMapper, dataMapper);
    }

    // ==================== 辅助方法 ====================

    private CourseSchedule createCourseSchedule(Long id, Long semesterId, Long courseId,
                                                   int startWeek, int endWeek,
                                                   int startHour, int startMinute) {
        CourseSchedule cs = new CourseSchedule();
        cs.setId(id);
        cs.setSemesterId(semesterId);
        cs.setLaboratoryId(1L);
        cs.setCourseId(courseId);
        cs.setWeekType(WeekType.Both);
        cs.setStartWeek(startWeek);
        cs.setEndWeek(endWeek);
        cs.setStartTime(LocalTime.of(startHour, startMinute));
        cs.setEndTime(LocalTime.of(startHour + 2, startMinute));
        cs.setWeekdays(Arrays.asList(1, 3, 5));
        cs.setStartSection(1);
        cs.setEndSection(2);
        return cs;
    }
}
