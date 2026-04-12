# Lab System Quartz Task Form - 定时任务表单组件

实验室管理系统 Quartz 定时任务配置的 VForm 风格表单组件设计指南。

---

## 业务概述

### 核心概念：条件-动作模型

```
ScheduleConfigRoot (任务配置根对象)
├── task: ScheduleTask           # 任务主体：何时触发（Cron）
├── timeRule: TimeRule           # 时间规则：何时期有效（学期/周/天/时段）
├── dataGroup: DataSource[]      # 数据源：读取哪些设备数据
├── conditionGroups: ConditionGroup[]  # 条件组：何时执行（SpEL条件）
├── actionGroups: ActionGroup[]  # 动作组：执行什么（设备控制指令）
├── alarmGroup: Alarm[]          # 报警配置：异常时通知谁
└── watchDog: WatchDog           # 看门狗：如何监控执行结果
```

**执行流程**：
```
Cron 触发 → 时间规则校验 → 条件组校验(SpEL) → 执行动作组 → 看门狗监控
```

### ID 关系与数据关联

所有子实体必须通过 `scheduleTaskId` 关联到任务主体：

```
task.id (雪花ID)
├── timeRule.scheduleTaskId = task.id
├── dataGroup[].scheduleTaskId = task.id
│   └── dataGroup[].id 用于 SpEL: #{id}.property
├── conditionGroups[].scheduleTaskId = task.id
│   └── conditions[].conditionGroupId = conditionGroups[].id
├── actionGroups[].scheduleTaskId = task.id
│   └── actions[].actionGroupId = actionGroups[].id
└── alarmGroup[].scheduleTaskId = task.id
```

**关键规则**：
- 所有实体使用雪花 ID（19位数字字符串）
- 子实体 `scheduleTaskId` 必须与 `task.id` 一致
- Condition 的 `conditionGroupId` = 所属 ConditionGroup 的 `id`
- Action 的 `actionGroupId` = 所属 ActionGroup 的 `id`

---

## SpEL 表达式规则

**格式**：`#{dataSourceId}.property operator value`

| 表达式示例 | 说明 |
|-----------|------|
| `#{1789569705678901234}.roomTemperature > 25` | 房间温度大于25度 |
| `#{1789569705678901234}.isOpen == false` | 空调处于关闭状态 |
| `#{1789569705678901234}.errorCode > 0` | 设备存在故障 |

**注意**：
- `dataSourceId` 是 `dataGroup` 中某数据源的 `id`
- 后端解析器会将 `#{id}` 转换为 `#root['id']` 访问 Map 数据

---

## TypeScript 类型定义

```typescript
// 基础枚举
enum DeviceType { AirCondition, CircuitBreak, Light, Sensor, Access }
enum CommandLine { OPEN_AIR_CONDITION_RS485, CLOSE_AIR_CONDITION_RS485, ENHANCE_CONTROL_AIR_CONDITION, ... }
enum ConditionGroupType { ALL, ANY }
enum AlarmType { SMS, SMTP }
enum WeekType { ALL, ODD, EVEN }

// 任务主体
interface ScheduleTask {
  id: string;              // 雪花ID
  taskName: string;
  cron: string;
  enable: boolean;
  startDate: string;       // yyyy-MM-dd
  endDate: string;
  laboratoryId: number;
}

// 动作
interface Action {
  id: string;
  deviceType: DeviceType;
  deviceId: number;
  commandLine: CommandLine;
  args: number[];
  actionGroupId: string;   // 【必须】关联动作组
  scheduleTaskId: string;  // 【必须】关联任务
}

// 动作组
interface ActionGroup {
  id: string;
  scheduleTaskId: string;  // 【必须】
  conditionGroupId?: string; // 关联条件组（可选）
  actions: Action[];
}

// 条件
interface Condition {
  id: string;
  expr: string;            // SpEL: #{id}.property > value
  desc: string;
  conditionGroupId: string;// 【必须】关联条件组
  scheduleTaskId: string;  // 【必须】
}

// 条件组
interface ConditionGroup {
  id: string;
  type: ConditionGroupType; // ALL=全部满足, ANY=任一满足
  scheduleTaskId: string;   // 【必须】
  conditions: Condition[];
}

// 数据源（用于SpEL引用）
interface DataSource {
  id: string;              // 【关键】SpEL中通过 #{id} 引用
  scheduleTaskId: string;  // 【必须】
  deviceId: number;
  deviceType: DeviceType;
}

// 时间规则
interface TimeRule {
  id: string;
  scheduleTaskId: string;  // 【必须】
  semesterId: number;
  weekdays: number[];      // [1,2,3,4,5,6,7]
  startWeek: number;
  endWeek: number;
  weekType: WeekType;
  startTime?: string;      // HH:mm:ss
  endTime?: string;
}

// 报警
interface Alarm {
  id: string;
  scheduleTaskId: string;  // 【必须】
  userId: number;
  type: AlarmType;
}

// 看门狗
interface WatchDog {
  watchEnabled: boolean;
  watchIntervalSec: number;
  watchTimeoutSec: number;
  stopOnFirstSuccess: boolean;
}

// 根对象
interface ScheduleConfigRoot {
  task: ScheduleTask;
  actionGroups: ActionGroup[];
  dataGroup: DataSource[];
  conditionGroups: ConditionGroup[];
  timeRule: TimeRule;
  alarmGroup: Alarm[];
  watchDog: WatchDog;
}
```

---

## 组件设计思想

### 1. 双向绑定设计

```
后端数据 → initFormData() → formData (reactive)
                              ↓
                           用户编辑
                              ↓
                           提交时同步 scheduleTaskId → 后端
```

**关键转换点**：
- `startDate/endDate` ↔ `dateRange`（日期范围选择器）
- `startTime/endTime` ↔ `timeRange`（时间范围选择器）

### 2. 新建 vs 编辑模式处理

```typescript
// 初始化逻辑
const initFormData = () => {
  if (props.initialValue) {
    // 编辑模式：保持所有 ID 不变，深拷贝恢复数据结构
    formData.task = { ...props.initialValue.task }
    formData.dataGroup = props.initialValue.dataGroup.map(d => ({ ...d }))
    // ... 其他字段同理
    
    // 恢复日期/时间范围绑定
    dateRange.value = [formData.task.startDate, formData.task.endDate]
    timeRange.value = [formData.timeRule.startTime, formData.timeRule.endTime]
  } else {
    // 新建模式：生成新 ID，设置默认值
    resetForm()
  }
}
```

### 3. ID 同步策略

提交前统一同步所有关联 ID：

```typescript
const handleSubmit = async () => {
  const taskId = formData.task.id
  
  // 同步 scheduleTaskId
  formData.timeRule.scheduleTaskId = taskId
  formData.dataGroup.forEach(d => d.scheduleTaskId = taskId)
  formData.conditionGroups.forEach(cg => {
    cg.scheduleTaskId = taskId
    cg.conditions.forEach(c => {
      c.scheduleTaskId = taskId
      c.conditionGroupId = cg.id  // 确保关联正确
    })
  })
  formData.actionGroups.forEach(ag => {
    ag.scheduleTaskId = taskId
    ag.actions.forEach(a => {
      a.scheduleTaskId = taskId
      a.actionGroupId = ag.id
    })
  })
  formData.alarmGroup.forEach(a => a.scheduleTaskId = taskId)
  
  await submit(formData)
}
```

---

## UI 布局建议

采用 **VForm 风格**：卡片式分组布局

```
┌─ 任务主体 ──────────────────────┐
│  任务名称 | Cron表达式           │
│  生效日期范围 | 启用开关         │
└─────────────────────────────────┘

┌─ 时间规则 ──────────────────────┐
│  学期选择 | 周次范围             │
│  单双周 | 生效星期（多选）       │
│  日内时段范围                    │
└─────────────────────────────────┘

┌─ 数据源配置 [+添加] ────────────┐
│  ┌─ 数据源1 ─────────────────┐  │
│  │ 设备选择 | 类型标签 | ID  |  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘

┌─ 条件配置 [+添加组] ────────────┐
│  ┌─ 条件组1 (全部满足/任一) ─┐  │
│  │  SpEL表达式 | 描述 | 删除 │  │
│  │  [+添加条件]              │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘

┌─ 动作配置 [+添加组] ────────────┐
│  ┌─ 动作组1 ────────────────┐   │
│  │ 关联条件组(可选) | 删除   │   │
│  │  设备 | 指令 | 参数 | 删除│   │
│  │  [+添加动作]             │   │
│  └───────────────────────────┘  │
└─────────────────────────────────┘

┌─ 报警配置 [+添加] ──────────────┐
│  用户选择 | 报警方式(短信/邮件)  │
└─────────────────────────────────┘

┌─ 看门狗配置 ────────────────────┐
│  启用开关                      │
│  检查间隔 | 超时时间 | 首次停止 │
└─────────────────────────────────┘

[取消] [重置(新建)] [创建/更新]
```

---

## 关键实现指引

### 1. 数据源与SpEL联动

当用户选择数据源设备后，在条件表达式中应可引用该数据源的 ID：

```typescript
// 数据源项显示其ID，供用户复制到SpEL中
<el-tag type="warning">ID: {{ data.id }}</el-tag>

// SpEL输入框提供快捷插入
<el-input v-model="cond.expr" placeholder="#{id}.property > value">
  <template #append>
    <el-dropdown @command="(id) => cond.expr = `#{${id}}.`">
      <el-button>插入数据源</el-button>
      <template #dropdown>
        <el-dropdown-item v-for="d in dataGroup" :command="d.id">
          设备{{ d.deviceId }} ({{ d.id.slice(-6) }})
        </el-dropdown-item>
      </template>
    </el-dropdown>
  </template>
</el-input>
```

### 2. 动作组关联条件组

```typescript
// 动作组头部提供条件组关联选择
<el-select v-model="group.conditionGroupId" clearable>
  <el-option 
    v-for="(cg, idx) in conditionGroups" 
    :label="`条件组${idx+1}`" 
    :value="cg.id" 
  />
</el-select>
```

### 3. 设备指令参数智能渲染

根据 `commandLine` 动态渲染参数输入：

```typescript
// ENHANCE_CONTROL_AIR_CONDITION: [address, selfId, 开关, 模式, 温度, 风速]
// 其他指令: [address, selfId]

const renderArgsInput = (command: CommandLine, device: Device) => {
  switch (command) {
    case CommandLine.ENHANCE_CONTROL_AIR_CONDITION:
      return <EnhancedControlArgs 
        v-model={args} 
        defaultAddress={device.address}
        defaultSelfId={device.selfId}
      />
    default:
      return <span>地址: {device.address}, 子ID: {device.selfId}</span>
  }
}
```

---

## API 接口

```typescript
// 创建任务（新建模式）
POST /quartz/create
body: ScheduleConfigRoot

// 更新任务（编辑模式）
PUT /quartz/update
body: ScheduleConfigRoot

// 获取任务详情（编辑/查看模式）
GET /quartz/detail?taskId={taskId}
response: ScheduleConfigRoot

// 其他操作
DELETE /quartz/delete?taskId={taskId}
POST /quartz/cancel?taskId={taskId}
POST /quartz/enable?taskId={taskId}
GET /quartz/list?enable={boolean}
GET /quartz/list-by-lab?laboratoryId={id}&...
```

---

## 注意事项

1. **ID 保持**：编辑模式下所有 ID 必须保持不变，否则后端会创建新记录
2. **scheduleTaskId 同步**：提交前确保所有子实体的 `scheduleTaskId` = `task.id`
3. **深拷贝**：初始化时使用深拷贝避免引用污染原始数据
4. **空值处理**：后端可能返回 null，提供合理的默认值
5. **日期格式**：前后端统一使用 `yyyy-MM-dd` 和 `HH:mm:ss` 格式
