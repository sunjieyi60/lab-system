# Quartz 任务配置隔离重构方案

## 1. 核心变更

**目标**：除 `ScheduleTask` 和 `TimeRule` 外，所有卡片（Data/Action/Condition/Alarm）**不再通过 `scheduleTaskId` 绑定到具体任务**，而是通过 `isolation` 字段组成**可复用模板**。

```
ScheduleTask (id=T001, isolation=template_A)
  ├── TimeRule (scheduleTaskId=T001)     ← 仍绑定任务实例，每个任务独立
  └── 引用模板 template_A
        ├── Data[] (isolation=template_A)
        ├── ConditionGroup[] (isolation=template_A)
        │   └── Condition[] (isolation=template_A)
        ├── ActionGroup[] (isolation=template_A)
        │   └── Action[] (isolation=template_A)
        └── Alarm[] (isolation=template_A)

ScheduleTask (id=T002, isolation=template_A)
  ├── TimeRule (scheduleTaskId=T002)     ← 独立的时间规则
  └── 引用同一套模板 template_A

ScheduleTask (id=T003, isolation=template_B)
  ├── TimeRule (scheduleTaskId=T003)
  └── 引用另一套模板 template_B
```

**一句话**：`isolation` = 模板ID，`scheduleTaskId` 仅用于 `TimeRule` 归属任务实例。

---

## 2. 实体字段改造

### 2.1 ScheduleTask（任务主体）

```java
public class ScheduleTask {
    private String id;              // 雪花ID，任务实例唯一标识
    private String isolation;       // 【新增】引用的模板标识
    private String taskName;
    private String cron;
    private Boolean enable;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long laboratoryId;
}
```

### 2.2 TimeRule（时间规则）— 保持不变

```java
public class TimeRule {
    private String id;
    private String scheduleTaskId;  // 【保留】仍绑定到具体任务实例
    private Long semesterId;
    private List<Integer> weekdays;
    private Integer startWeek;
    private Integer endWeek;
    private WeekType weekType;
    private LocalTime startTime;
    private LocalTime endTime;
}
```

### 2.3 Data（数据源）— 移除 scheduleTaskId，改为 isolation

```java
public class Data {
    private String id;
    // private String scheduleTaskId;   // 【移除】
    private String isolation;           // 【新增】模板标识
    private Long deviceId;
    private DeviceType deviceType;
    private DeviceRecordVo<? extends BaseRecord> value;
}
```

### 2.4 ConditionGroup（条件组）— 移除 scheduleTaskId，改为 isolation

```java
public class ConditionGroup {
    private String id;
    private ConditionGroupType type;
    // private String scheduleTaskId;   // 【移除】
    private String isolation;           // 【新增】模板标识
    private List<Condition> conditions;
}
```

### 2.5 Condition（条件）— 移除 scheduleTaskId，改为 isolation

```java
public class Condition {
    private String id;
    private String expr;
    private String desc;
    private String conditionGroupId;    // 【保留】仍关联所属条件组
    // private String scheduleTaskId;   // 【移除】
    private String isolation;           // 【新增】模板标识
}
```

### 2.6 ActionGroup（动作组）— 移除 scheduleTaskId，改为 isolation

```java
public class ActionGroup {
    private String id;
    // private String scheduleTaskId;   // 【移除】
    private String isolation;           // 【新增】模板标识
    private String conditionGroupId;    // 【保留】关联条件组（跨模板时可能为空）
    private List<Action> actions;
}
```

### 2.7 Action（动作）— 移除 scheduleTaskId，改为 isolation

```java
public class Action {
    private String id;
    private DeviceType deviceType;
    private Long deviceId;
    private CommandLine commandLine;
    private Integer[] args;
    private String actionGroupId;       // 【保留】仍关联所属动作组
    // private String scheduleTaskId;   // 【移除】
    private String isolation;           // 【新增】模板标识
}
```

### 2.8 Alarm（报警）— 移除 scheduleTaskId，改为 isolation

```java
public class Alarm {
    private String id;
    // private String scheduleTaskId;   // 【移除】
    private String isolation;           // 【新增】模板标识
    private Long userId;
    private AlarmType type;
}
```

### 2.9 ScheduleConfigRoot（前端提交根对象）

```java
public class ScheduleConfigRoot {
    private ScheduleTask task;              // task.isolation 指定模板
    private TimeRule timeRule;              // timeRule.scheduleTaskId 绑定任务
    private List<ActionGroup> actionGroups; // actionGroups[*].isolation 指定模板
    private List<Data> dataGroup;
    private List<ConditionGroup> conditionGroups;
    private List<Alarm> alarmGroup;
    private WatchDog watchDog;
}
```

---

## 3. 数据库变更

### 3.1 新增字段

```sql
-- ScheduleTask 新增 isolation
ALTER TABLE schedule_task ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_schedule_task_isolation ON schedule_task(isolation);

-- Data 新增 isolation，废弃 schedule_task_id
ALTER TABLE `data` ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_data_isolation ON `data`(isolation);
-- ALTER TABLE `data` DROP COLUMN schedule_task_id;   -- 视迁移情况执行

-- ConditionGroup 新增 isolation
ALTER TABLE condition_group ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
-- ALTER TABLE condition_group DROP COLUMN schedule_task_id;

-- Condition 新增 isolation
ALTER TABLE `condition` ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_condition_isolation ON `condition`(isolation);
-- ALTER TABLE `condition` DROP COLUMN schedule_task_id;

-- ActionGroup 新增 isolation
ALTER TABLE action_group ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
-- ALTER TABLE action_group DROP COLUMN schedule_task_id;

-- Action 新增 isolation
ALTER TABLE `action` ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_action_isolation ON `action`(isolation);
-- ALTER TABLE `action` DROP COLUMN schedule_task_id;

-- Alarm 新增 isolation
ALTER TABLE alarm ADD COLUMN isolation VARCHAR(64) NULL COMMENT '模板隔离标识';
CREATE INDEX idx_alarm_isolation ON alarm(isolation);
-- ALTER TABLE alarm DROP COLUMN schedule_task_id;
```

### 3.2 数据迁移建议（存量数据）

若已有数据全部是一对一（每个 task 一套独立卡片），迁移脚本：

```sql
-- 将存量 task.id 复制到各卡片的 isolation 字段，实现兼容
UPDATE schedule_task SET isolation = id WHERE isolation IS NULL;

UPDATE `data` d
SET isolation = (SELECT id FROM schedule_task t WHERE t.id = d.schedule_task_id)
WHERE isolation IS NULL;

UPDATE condition_group cg
SET isolation = (SELECT id FROM schedule_task t WHERE t.id = cg.schedule_task_id)
WHERE isolation IS NULL;

-- ... Condition、ActionGroup、Action、Alarm 同理
```

迁移后，每个旧任务变成「自己引用自己的模板」，逻辑仍然正确。

---

## 4. 后端逻辑改造

### 4.1 保存逻辑（ConfigLoader）

```java
public Result<Boolean> configCreate(ScheduleConfigRoot root) {
    ScheduleTask task = root.getTask();
    String taskId = IdUtil.getSnowflakeNextIdStr();
    task.setId(taskId);

    // 若未指定 isolation，自动生成（首次创建即创建新模板）
    String isolation = task.getIsolation();
    if (isolation == null || isolation.isBlank()) {
        isolation = IdUtil.getSnowflakeNextIdStr();
        task.setIsolation(isolation);
    }

    // 1. 保存任务主体
    scheduleTaskMapper.insert(task);

    // 2. 保存 TimeRule（绑定任务实例）
    TimeRule tr = root.getTimeRule();
    tr.setScheduleTaskId(taskId);
    timeRuleMapper.insert(tr);

    // 3. 保存模板卡片（绑定 isolation，不绑定 scheduleTaskId）
    saveTemplateCards(isolation, root);

    return Result.success(true);
}

private void saveTemplateCards(String isolation, ScheduleConfigRoot root) {
    // Data
    for (Data d : root.getDataGroup()) {
        d.setIsolation(isolation);
        // d.setScheduleTaskId(null);  // 确保不绑任务
        dataMapper.insert(d);
    }

    // ConditionGroup + Condition
    for (ConditionGroup cg : root.getConditionGroups()) {
        cg.setIsolation(isolation);
        conditionGroupMapper.insert(cg);
        for (Condition c : cg.getConditions()) {
            c.setIsolation(isolation);
            c.setConditionGroupId(cg.getId());
            conditionMapper.insert(c);
        }
    }

    // ActionGroup + Action
    for (ActionGroup ag : root.getActionGroups()) {
        ag.setIsolation(isolation);
        actionGroupMapper.insert(ag);
        for (Action a : ag.getActions()) {
            a.setIsolation(isolation);
            a.setActionGroupId(ag.getId());
            actionMapper.insert(a);
        }
    }

    // Alarm
    for (Alarm alarm : root.getAlarmGroup()) {
        alarm.setIsolation(isolation);
        alarmMapper.insert(alarm);
    }
}
```

### 4.2 查询组装逻辑（运行时 / 详情接口）

```java
public ScheduleConfigRoot assembleConfig(String scheduleTaskId) {
    // 1. 查任务主体，获取 isolation
    ScheduleTask task = scheduleTaskMapper.selectById(scheduleTaskId);
    String isolation = task.getIsolation();

    // 2. 查 TimeRule（按 scheduleTaskId）
    TimeRule timeRule = timeRuleMapper.selectByTaskId(scheduleTaskId);

    // 3. 查模板卡片（按 isolation）
    List<Data> dataGroup = dataMapper.selectByIsolation(isolation);
    List<ConditionGroup> conditionGroups = conditionGroupMapper.selectByIsolationWithConditions(isolation);
    List<ActionGroup> actionGroups = actionGroupMapper.selectByIsolationWithActions(isolation);
    List<Alarm> alarmGroup = alarmMapper.selectByIsolation(isolation);

    // 4. 组装
    ScheduleConfigRoot root = new ScheduleConfigRoot();
    root.setTask(task);
    root.setTimeRule(timeRule);
    root.setDataGroup(dataGroup);
    root.setConditionGroups(conditionGroups);
    root.setActionGroups(actionGroups);
    root.setAlarmGroup(alarmGroup);
    return root;
}
```

### 4.3 课表批量生成逻辑改进

```java
public R<Boolean> generateScheduleTask(CourseScheduleTaskGenerator target) {
    String isolation = target.getIsolation();  // 前端传入要复用的模板

    for (CourseSchedule cs : courseSchedules) {
        // 1. 生成任务主体
        ScheduleTask task = new ScheduleTask();
        String taskId = IdUtil.getSnowflakeNextIdStr();
        task.setId(taskId);
        task.setIsolation(isolation);   // 关联模板
        // ... 名称、cron、日期 ...
        scheduleTaskMapper.insert(task);

        // 2. 生成 TimeRule（独立，绑定任务实例）
        TimeRule tr = TimeRule.courseSchedule2TimeRule(cs);
        tr.setScheduleTaskId(taskId);
        // ... 提前/延迟 ...
        timeRuleMapper.insert(tr);

        // 3. Data/Action/Condition/Alarm 完全不生成，执行时通过 isolation 复用模板
    }

    return R.success(true, "成功生成 " + courseSchedules.size() + " 个定时任务");
}
```

> **注意**：课表导入只生成 `ScheduleTask` + `TimeRule`，卡片全部复用指定模板。

---

## 5. 前端影响

### 5.1 表单提交

提交 `ScheduleConfigRoot` 时：
- `task.isolation`：若用户选择了已有模板，传入模板ID；若新建，传空
- `timeRule.scheduleTaskId`：提交前同步为 `task.id`
- 卡片（Data/ActionGroup/ConditionGroup/Alarm）中的 `isolation`：提交前同步为 `task.isolation`
- 卡片中的 `scheduleTaskId`：不再需要，可为空

### 5.2 模板选择器

在「根据课表生成定时任务」页面增加模板选择：

```
┌─────────────────────────────────────────────┐
│  根据课表生成定时任务                          │
├─────────────────────────────────────────────┤
│  1. 选择实验室        [多选]                  │
│  2. 选择动作模板      [下拉框]                │
│     - 开空调自动控制 (isolation=xxx)          │
│     - 开灯自动控制   (isolation=yyy)          │
│     - 不选模板（仅生成空任务+时间规则）        │
│  3. cron / 提前 / 延迟 / 启用                 │
└─────────────────────────────────────────────┘
```

### 5.3 模板管理页面

新增 `/quartz/template` 相关接口：

```http
GET    /quartz/template/list          // 列出所有模板（按 isolation 分组）
GET    /quartz/template/detail?isolation={id}  // 查看模板详情
POST   /quartz/template/create        // 创建新模板（传 ScheduleConfigRoot，不含 task/timeRule）
PUT    /quartz/template/update        // 更新模板（会同步影响所有引用该模板的任务）
DELETE /quartz/template/delete?isolation={id}  // 删除模板（需校验是否有任务引用）
```

---

## 6. 关键边界处理

### 6.1 模板编辑的影响范围

修改模板卡片时，**所有引用该 `isolation` 的任务都会受影响**。
- 若只想改某个任务，应为其生成新的 `isolation`（复制模板）
- 前端编辑任务时，提供「保存为新模板」选项

### 6.2 ActionGroup 关联 ConditionGroup

`actionGroup.conditionGroupId` 仍保留，但需注意：
- 若 ActionGroup 和 ConditionGroup 在同一模板内，`conditionGroupId` 指向同模板下的条件组
- 若跨模板关联，需要额外处理（建议限制同模板内关联）

### 6.3 SpEL 中的 dataId

`Condition.expr` 中的 `#{dataId}.property`：
- `dataId` 是 `Data.id`（雪花ID），同一模板内的 Data ID 固定
- 多个任务共享同一模板时，SpEL 引用的 dataId 不变，正确

### 6.4 删除任务时的清理

删除 `ScheduleTask` 时：
- 删除自身的 `TimeRule`
- **不删除**模板卡片（可能被其他任务引用）
- 如需清理孤儿模板，提供后台定时任务或手动清理接口

---

## 7. 待办清单

| 序号 | 任务 | 文件 |
|------|------|------|
| 1 | Model 实体移除 `scheduleTaskId`，新增 `isolation` | `Data`, `Condition`, `ConditionGroup`, `Action`, `ActionGroup`, `Alarm` |
| 2 | `ScheduleTask` 新增 `isolation` | `ScheduleTask` |
| 3 | 数据库 DDL（加字段、建索引） | `schema-h2.sql`, `db.sql` |
| 4 | 存量数据迁移脚本 | 迁移 SQL |
| 5 | Mapper 层改按 `isolation` 查询 | `DataMapper`, `ConditionMapper`, `ActionMapper`, `AlarmMapper` |
| 6 | `ConfigLoader.configCreate` 改保存逻辑 | `ConfigLoader.java` |
| 7 | `TaskQueryService` 改组装逻辑 | `TaskQueryService.java` |
| 8 | `TaskGeneratorService.generateScheduleTask` 改生成逻辑 | `TaskGeneratorService.java` |
| 9 | 新增模板管理接口 | `QuartzController.java` |
| 10 | 前端模板选择器 + 模板管理页面 | Vue/React 组件 |
