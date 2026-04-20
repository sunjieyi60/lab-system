# Quartz 定时任务隔离（isolation）改进方案

## 1. 问题背景

当前 `TaskGeneratorService.generateScheduleTask()` 根据课表批量生成定时任务时，**仅生成了 `ScheduleTask` + `TimeRule`**：

```java
scheduleTaskMapper.insert(scheduleTask);   // 任务主体
timeRuleMapper.insert(timeRule);           // 时间规则
```

但一个完整的 `ScheduleConfigRoot` 还包含：
- `dataGroup` — 数据源（监控哪些设备数据）
- `actionGroups` — 动作组（满足条件后执行什么指令）
- `conditionGroups` — 条件组（判断逻辑）
- `alarmGroup` — 报警配置

如果直接按单个任务独立生成这些卡片，会出现以下问题：
1. **ID 爆炸**：每个课表任务都生成一套独立的 Action/Condition/Data，数据库快速膨胀
2. **维护困难**：相同逻辑（如「开空调到26度」）散落在无数条记录中，修改需逐条调整
3. **复用缺失**：不同实验室、不同课程可能共享相同的动作模板，但当前无法复用

---

## 2. 核心设计思路

引入 **`isolation`（隔离标识 / 模板组）** 字段，将「**卡片模板**」与「**任务实例**」解耦：

| 概念 | 说明 |
|------|------|
| **模板（isolation）** | 一组可复用的 Data、Action、Condition、Alarm 卡片，共享同一个 `isolation` 值 |
| **任务实例** | 每个课表任务独立的 `ScheduleTask` + `TimeRule`，通过 `isolation` 字段关联到模板 |

### 2.1 数据分层

```
isolation = "lab_auto_ac"          <-- 模板组（一套标准动作）
├── Data[]          {deviceType=AIR, deviceId=1001}
├── ConditionGroup  {type=ALL}
│   └── Condition[] {expr="#{data.xxx}.temperature >= 30"}
├── ActionGroup
│   └── Action[]    {deviceType=AIR, commandLine=SET_TEMP, args=[26]}
└── Alarm[]         {type=SMTP, userId=1}

Task Instance A    <-- 课表任务实例
├── ScheduleTask    {id="task_001", isolation="lab_auto_ac", ...}
└── TimeRule        {scheduleTaskId="task_001", weekdays=[1,3], ...}

Task Instance B    <-- 另一个课表任务，复用同一套模板
├── ScheduleTask    {id="task_002", isolation="lab_auto_ac", ...}
└── TimeRule        {scheduleTaskId="task_002", weekdays=[2,4], ...}
```

### 2.2 关键原则

1. **模板复用**：相同 `isolation` 的 Data/Action/Condition/Alarm 被所有关联任务共享
2. **时间独立**：每个任务的 `ScheduleTask` + `TimeRule` 独立生成（时间各不相同）
3. **执行组装**：运行时根据 `scheduleTask.isolation` 查询模板卡片，与 TimeRule 组装成完整的 `ScheduleConfigRoot`

---

## 3. 实体变更

给 `ScheduleConfigRoot` 涉及的所有实体添加 `isolation` 字段：

### 3.1 模型层（Model）

```java
// ScheduleTask.java
private String isolation;   // 关联的模板隔离标识

// ActionGroup.java
private String isolation;   // 所属模板组

// Action.java
private String isolation;   // 所属模板组

// Data.java
private String isolation;   // 所属模板组

// ConditionGroup.java
private String isolation;   // 所属模板组

// Condition.java
private String isolation;   // 所属模板组

// Alarm.java
private String isolation;   // 所属模板组
```

### 3.2 根配置（ScheduleConfigRoot）

前端提交时新增字段：

```java
public class ScheduleConfigRoot {
    // ... 原有字段 ...

    /**
     * 隔离标识（模板组 ID）。
     * 若传入已有 isolation，则复用该模板组下的 Data/Action/Condition/Alarm；
     * 若传入新值或空值，则创建新的模板组。
     */
    private String isolation;
}
```

### 3.3 课表生成器（CourseScheduleTaskGenerator）

支持指定模板：

```java
public class CourseScheduleTaskGenerator {
    // ... 原有字段 ...

    /**
     * 要复用的模板隔离标识。
     * 若指定，则生成的任务会关联该模板；
     * 若为空，则只生成 ScheduleTask + TimeRule（保持向后兼容）。
     */
    private String isolation;
}
```

---

## 4. 数据库变更

给所有相关表增加 `isolation` 字段和索引：

```sql
-- 任务主体表
ALTER TABLE schedule_task ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_schedule_task_isolation ON schedule_task(isolation);

-- 数据源表
ALTER TABLE `data` ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_data_isolation ON `data`(isolation);

-- 动作组表
ALTER TABLE action_group ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';

-- 动作表
ALTER TABLE `action` ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_action_isolation ON `action`(isolation);

-- 条件组表
ALTER TABLE condition_group ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';

-- 条件表
ALTER TABLE `condition` ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_condition_isolation ON `condition`(isolation);

-- 报警表
ALTER TABLE alarm ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_alarm_isolation ON alarm(isolation);
```

> **注意**：`TimeRule` **不需要** `isolation`，因为每个任务的时间规则是独立的，不复用。

---

## 5. 生成逻辑改进

### 5.1 当前逻辑（仅生成时间）

```java
for (CourseSchedule cs : courseSchedules) {
    ScheduleTask task = new ScheduleTask();
    task.setId(IdUtil.getSnowflakeNextIdStr());
    // ... 设置名称、cron、日期 ...
    TimeRule tr = TimeRule.courseSchedule2TimeRule(cs);
    scheduleTaskMapper.insert(task);
    timeRuleMapper.insert(tr);
}
```

### 5.2 改进后逻辑（关联模板）

```java
public R<Boolean> generateScheduleTask(CourseScheduleTaskGenerator target) {
    String isolation = target.getIsolation();  // 复用的模板标识

    for (CourseSchedule cs : courseSchedules) {
        // 1. 生成任务主体（独立）
        ScheduleTask task = new ScheduleTask();
        String taskId = IdUtil.getSnowflakeNextIdStr();
        task.setId(taskId);
        task.setIsolation(isolation);   // <-- 关联模板
        // ... 设置名称、cron、日期 ...
        scheduleTaskMapper.insert(task);

        // 2. 生成时间规则（独立，不复用）
        TimeRule tr = TimeRule.courseSchedule2TimeRule(cs);
        tr.setScheduleTaskId(taskId);
        // ... 提前/延迟调整 ...
        timeRuleMapper.insert(tr);
    }

    return R.success(true, "成功生成 " + courseSchedules.size() + " 个定时任务");
}
```

### 5.3 模板创建逻辑（手动 / 首次导入）

当 `isolation` 为空或不存在时，由前端提交完整的 `ScheduleConfigRoot`，后端保存模板卡片：

```java
// ConfigLoader.configCreate 改进
public Result<Boolean> configCreate(ScheduleConfigRoot root) {
    String isolation = root.getIsolation();
    if (isolation == null || isolation.isBlank()) {
        isolation = IdUtil.getSnowflakeNextIdStr();  // 自动生成模板标识
        root.setIsolation(isolation);
    }

    // 保存任务主体
    ScheduleTask task = root.getTask();
    task.setIsolation(isolation);
    scheduleTaskMapper.insert(task);

    // 保存时间规则（独立，不复用）
    TimeRule tr = root.getTimeRule();
    tr.setScheduleTaskId(task.getId());
    timeRuleMapper.insert(tr);

    // 保存模板卡片（带 isolation，可被复用）
    saveTemplateCards(isolation, root.getDataGroup(), root.getActionGroups(),
                      root.getConditionGroups(), root.getAlarmGroup());

    return Result.success(true);
}
```

---

## 6. 执行逻辑改进

运行时（Quartz Job 执行或配置查询）需要根据 `scheduleTaskId` 组装完整的 `ScheduleConfigRoot`：

```java
public ScheduleConfigRoot assembleConfig(String scheduleTaskId) {
    // 1. 查询任务主体
    ScheduleTask task = scheduleTaskMapper.selectById(scheduleTaskId);

    // 2. 查询独立的时间规则
    TimeRule timeRule = timeRuleMapper.selectByTaskId(scheduleTaskId);

    // 3. 根据 isolation 查询模板卡片（复用部分）
    String isolation = task.getIsolation();
    List<Data> dataGroup = dataMapper.selectByIsolation(isolation);
    List<ActionGroup> actionGroups = actionGroupMapper.selectByIsolation(isolation);
    List<ConditionGroup> conditionGroups = conditionGroupMapper.selectByIsolation(isolation);
    List<Alarm> alarmGroup = alarmMapper.selectByIsolation(isolation);

    // 4. 组装完整的 ScheduleConfigRoot
    ScheduleConfigRoot root = new ScheduleConfigRoot();
    root.setTask(task);
    root.setTimeRule(timeRule);
    root.setDataGroup(dataGroup);
    root.setActionGroups(actionGroups);
    root.setConditionGroups(conditionGroups);
    root.setAlarmGroup(alarmGroup);
    root.setIsolation(isolation);

    return root;
}
```

---

## 7. 前端影响

### 7.1 课表批量导入页面

当前 `/generate-from-course-schedule` 接口只需选择实验室，改进后增加「**选择动作模板**」步骤：

```
┌─────────────────────────────────────────────┐
│  根据课表生成定时任务                          │
├─────────────────────────────────────────────┤
│  1. 选择实验室        [多选下拉框]             │
│  2. 选择动作模板      [下拉框 / 留空不生成]    │
│     - 空调自动控制 (isolation=lab_auto_ac)    │
│     - 灯光自动控制 (isolation=lab_auto_light) │
│     - 不关联模板 (仅生成时间规则)              │
│  3. cron 表达式      [默认 0 0/5 * * * ?]    │
│  4. 提前/延迟分钟    [7 / 7]                  │
│  5. 是否启用         [是]                     │
└─────────────────────────────────────────────┘
```

### 7.2 模板管理页面（新增）

需要新增一个「**定时任务模板管理**」页面，供用户维护可复用的 Data/Action/Condition/Alarm 卡片：

- 展示所有 `isolation` 分组
- 每个分组内可查看/编辑：数据源、动作、条件、报警
- 支持「基于现有任务创建模板」

### 7.3 定时任务列表

任务列表中增加「**关联模板**」列，方便查看哪些任务共享同一套动作逻辑。

---

## 8. 待办清单

| 序号 | 任务 | 涉及文件 |
|------|------|----------|
| 1 | 给所有 Model 添加 `isolation` 字段 | `ScheduleTask`, `ActionGroup`, `Action`, `Data`, `ConditionGroup`, `Condition`, `Alarm`, `ScheduleConfigRoot`, `CourseScheduleTaskGenerator` |
| 2 | 数据库 DDL 变更 | `schedule_task`, `data`, `action_group`, `action`, `condition_group`, `condition`, `alarm` 表加字段和索引 |
| 3 | Mapper 层增加 `selectByIsolation` 方法 | `DataMapper`, `ActionGroupMapper`, `ActionMapper`, `ConditionGroupMapper`, `ConditionMapper`, `AlarmMapper` |
| 4 | 修改 `ConfigLoader.configCreate` | 支持保存模板卡片（带 isolation） |
| 5 | 修改 `TaskGeneratorService.generateScheduleTask` | 生成的任务关联指定 isolation |
| 6 | 修改 `TaskQueryService` / `TaskRuntimeService` | 运行时根据 isolation 组装完整配置 |
| 7 | 修改 `QuartzRegister` / Job 执行逻辑 | 执行前调用 `assembleConfig` 获取完整配置 |
| 8 | 前端新增模板选择器和模板管理页面 | `CourseScheduleTaskGenerator` 接口、新增模板管理路由 |

---

## 9. 向后兼容

- `isolation` 字段默认为 `NULL`
- 旧数据（无 isolation）的任务执行时，按原有逻辑处理（仅查询自身关联的卡片）
- `CourseScheduleTaskGenerator.isolation` 为空时，只生成 `ScheduleTask` + `TimeRule`，保持与现有行为一致
