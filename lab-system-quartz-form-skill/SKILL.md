# Lab System Quartz 定时任务表单 Skill

## 概述

本文档描述了实验室综合管理系统中 Quartz 定时任务模块的完整数据模型、API 接口和业务规则，用于构建前端 Vue + TypeScript 组件。

---

## 数据模型

### 1. 根配置对象 ScheduleConfigRoot

定时任务的完整配置根对象，包含任务的所有关联配置。

```typescript
interface ScheduleConfigRoot {
  /** 任务主体 */
  task: ScheduleTask;
  /** 动作组列表 */
  actionGroups?: ActionGroup[];
  /** 数据组列表（数据源定义） */
  dataGroup?: Data[];
  /** 条件组列表 */
  conditionGroups?: ConditionGroup[];
  /** 时间规则 */
  timeRule?: TimeRule;
  /** 报警配置列表 */
  alarmGroup?: Alarm[];
  /** 看门狗配置 */
  watchDog?: WatchDog;
}
```

### 2. 任务主体 ScheduleTask

```typescript
interface ScheduleTask {
  /** 任务ID（唯一标识，String类型） */
  id: string;
  /** 任务名称 */
  taskName: string;
  /** Cron表达式 */
  cron: string;
  /** 是否启用（默认true） */
  enable?: boolean;
  /** 开始日期（YYYY-MM-DD） */
  startDate: string; // LocalDate
  /** 结束日期（YYYY-MM-DD） */
  endDate: string; // LocalDate
  /** 所属实验室ID */
  laboratoryId: number;
}
```

**Cron 表达式示例：**
| 描述 | Cron 表达式 |
|------|------------|
| 每5分钟执行 | `0 0/5 * * * ?` |
| 每小时执行 | `0 0 * * * ?` |
| 每天8点执行 | `0 0 8 * * ?` |
| 每分钟执行 | `0 * * * * ?` |

### 3. 动作组 ActionGroup

动作组关联一个条件组，当条件满足时执行组内的动作列表。

```typescript
interface ActionGroup {
  /** 动作组ID */
  id?: string;
  /** 调度任务ID */
  scheduleTaskId?: string;
  /** 关联的条件组ID（当该条件组满足时执行本组动作） */
  conditionGroupId: string;
  /** 动作列表 */
  actions?: Action[];
}
```

### 4. 动作 Action

动作定义设备控制指令。

```typescript
interface Action {
  /** 动作ID */
  id?: string;
  /** 设备类型 */
  deviceType: DeviceType;
  /** 设备ID */
  deviceId: number;
  /** 指令类型 */
  commandLine: CommandLine;
  /** 参数列表 [address, selfId, ...操作参数] */
  args: number[];
  /** 所属动作组ID */
  actionGroupId?: string;
  /** 调度任务ID */
  scheduleTaskId?: string;
}
```

### 5. 设备类型 DeviceType

```typescript
enum DeviceType {
  /** 空调 */
  AirCondition = 'AirCondition',
  /** 断路器/电气设备 */
  CircuitBreak = 'CircuitBreak',
  /** 照明 */
  Light = 'Light',
  /** 环境传感器 */
  Sensor = 'Sensor',
  /** 门禁 */
  Access = 'Access'
}
```

**设备类型属性对照：**
| 设备类型 | 起始地址 | 结束地址 | Redis前缀 |
|---------|---------|---------|----------|
| AirCondition | 0x1F (31) | 0x28 (40) | AirCondition:Record: |
| CircuitBreak | 0x0B (11) | 0x1E (30) | CircuitBreak:Record: |
| Light | 0x29 (41) | 0x3C (60) | Light:Record: |
| Sensor | 0x3D (61) | 0x50 (80) | Sensor:Record: |
| Access | 0x01 (1) | 0x0A (10) | Access:Record: |

### 6. 指令类型 CommandLine

```typescript
enum CommandLine {
  // 空调指令
  OPEN_AIR_CONDITION_RS485 = 'OPEN_AIR_CONDITION_RS485',
  CLOSE_AIR_CONDITION_RS485 = 'CLOSE_AIR_CONDITION_RS485',
  ENHANCE_CONTROL_AIR_CONDITION = 'ENHANCE_CONTROL_AIR_CONDITION',
  REQUEST_AIR_CONDITION_DATA_RS485 = 'REQUEST_AIR_CONDITION_DATA_RS485',
  REQUEST_AIR_CONDITION_DATA_SOCKET = 'REQUEST_AIR_CONDITION_DATA_SOCKET',

  // 门禁指令
  OPEN_ACCESS_ONCE = 'OPEN_ACCESS_ONCE',
  CLOSE_ACCESS_ONCE = 'CLOSE_ACCESS_ONCE',
  REQUEST_ACCESS_DATA = 'REQUEST_ACCESS_DATA',
  OPEN_ACCESS_PERSIST_LOCK = 'OPEN_ACCESS_PERSIST_LOCK',
  OPEN_ACCESS_PERSIST_UNLOCK = 'OPEN_ACCESS_PERSIST_UNLOCK',
  OPEN_ACCESS_PERSIST_KEEP = 'OPEN_ACCESS_PERSIST_KEEP',
  CLOSE_ACCESS_PERSIST_LOCK = 'CLOSE_ACCESS_PERSIST_LOCK',
  CLOSE_ACCESS_PERSIST_UNLOCK = 'CLOSE_ACCESS_PERSIST_UNLOCK',
  CLOSE_ACCESS_PERSIST_KEEP = 'CLOSE_ACCESS_PERSIST_KEEP',
  KEEP_ACCESS_STATUS_LOCK = 'KEEP_ACCESS_STATUS_LOCK',
  KEEP_ACCESS_STATUS_UNLOCK = 'KEEP_ACCESS_STATUS_UNLOCK',
  SET_ACCESS_DELAY = 'SET_ACCESS_DELAY',

  // 断路器指令
  OPEN_CIRCUITBREAK = 'OPEN_CIRCUITBREAK',
  CLOSE_CIRCUITBREAK = 'CLOSE_CIRCUITBREAK',
  REQUEST_CIRCUITBREAK_DATA = 'REQUEST_CIRCUITBREAK_DATA',

  // 照明指令
  OPEN_LIGHT = 'OPEN_LIGHT',
  CLOSE_LIGHT = 'CLOSE_LIGHT',
  REQUEST_LIGHT_DATA = 'REQUEST_LIGHT_DATA',

  // 传感器指令
  REQUEST_SENSOR_DATA = 'REQUEST_SENSOR_DATA'
}
```

**指令参数说明：**
| 指令 | 参数说明 | 参数示例 |
|-----|---------|---------|
| ENHANCE_CONTROL_AIR_CONDITION | [address, selfId, 开关, 模式, 温度, 风速] | [35, 1, 1, 2, 26, 1] |
| OPEN_AIR_CONDITION_RS485 | [address, selfId] | [35, 1] |
| CLOSE_AIR_CONDITION_RS485 | [address, selfId] | [35, 1] |
| OPEN_LIGHT | [address, selfId] | [41, 1] |
| CLOSE_LIGHT | [address, selfId] | [41, 1] |
| OPEN_ACCESS_ONCE | [address, selfId] | [5, 1] |
| OPEN_CIRCUITBREAK | [address] | [7] |

**空调增强控制参数详解：**
- 开关: 0=关, 1=开
- 模式: 1=制热, 2=制冷, 4=送风, 8=除湿
- 温度: 16-30℃ 整数
- 风速: 0=自动, 1=低风, 2=中风, 3=高风

### 7. 条件组 ConditionGroup

```typescript
interface ConditionGroup {
  /** 条件组ID */
  id?: string;
  /** 条件组类型：ALL-全部满足, ANY-任一满足 */
  type: ConditionGroupType;
  /** 调度任务ID */
  scheduleTaskId?: string;
  /** 条件列表 */
  conditions?: Condition[];
}

enum ConditionGroupType {
  /** 所有条件都满足 */
  ALL = 'ALL',
  /** 任一条件满足 */
  ANY = 'ANY'
}
```

### 8. 条件 Condition

条件使用 SpEL 表达式进行数据判断。

```typescript
interface Condition {
  /** 条件ID */
  id?: string;
  /** 条件表达式（SpEL格式） */
  expr: string;
  /** 条件描述 */
  desc?: string;
  /** 所属条件组ID */
  conditionGroupId?: string;
  /** 调度任务ID */
  scheduleTaskId?: string;
}
```

**SpEL 表达式格式：**
```
#{dataId}.property operator value
```

**表达式示例：**
| 场景 | 表达式 |
|-----|--------|
| 空调温度低于18度 | `#{1789569705678901234}.roomTemperature <= 18` |
| 空调关闭状态 | `#{1789569705678901234}.isOpen == false` |
| 空调故障 | `#{1789569705678901234}.errorCode > 0` |
| 室温高于30度 | `#{1789569705678901234}.roomTemperature > 30` |

**可用数据属性（按设备类型）：**

空调 (AirConditionRecord):
- `isOpen`: boolean - 开关状态
- `mode`: string - 模式
- `temperature`: number - 设定温度
- `speed`: string - 风速
- `roomTemperature`: number - 室温
- `errorCode`: number - 故障码

断路器 (CircuitBreakRecord):
- `isOpen`: boolean - 开关状态
- `isFix`: boolean - 复位状态
- `isLock`: boolean - 锁定状态
- `voltage`: number - 电压
- `current`: number - 电流
- `power`: number - 功率
- `energy`: number - 电能
- `leakage`: number - 漏电流
- `temperature`: number - 温度

照明 (LightRecord):
- `isOpen`: boolean - 开关状态
- `isLock`: boolean - 锁定状态

传感器 (SensorRecord):
- `temperature`: number - 温度
- `humidity`: number - 湿度
- `light`: number - 光照
- `smoke`: number - 烟雾

门禁 (AccessRecord):
- `isOpen`: boolean - 门状态
- `isLock`: boolean - 锁定状态
- `lockStatus`: number - 锁状态
- `delayTime`: number - 延时

### 9. 数据源 Data

```typescript
interface Data {
  /** 数据ID（在表达式中引用） */
  id?: string;
  /** 调度任务ID */
  scheduleTaskId?: string;
  /** 设备ID */
  deviceId: number;
  /** 设备类型 */
  deviceType: DeviceType;
  /** 设备实时数据（运行时填充） */
  value?: DeviceRecordVo;
}
```

### 10. 时间规则 TimeRule

```typescript
interface TimeRule {
  /** 规则ID */
  id?: string;
  /** 调度任务ID */
  scheduleTaskId?: string;
  /** 学期ID */
  semesterId?: number;
  /** 星期列表 [1,2,3,4,5,6,7] 代表周一至周日 */
  weekdays: number[];
  /** 开始周（第几周开始） */
  startWeek: number;
  /** 结束周（第几周结束） */
  endWeek: number;
  /** 单双周类型 */
  weekType: WeekType;
  /** 开始时间（HH:mm:ss） */
  startTime: string; // LocalTime
  /** 结束时间（HH:mm:ss） */
  endTime: string; // LocalTime
}

enum WeekType {
  /** 单周 */
  Single = 'Single',
  /** 双周 */
  Double = 'Double',
  /** 全部周 */
  Both = 'Both'
}
```

### 11. 报警配置 Alarm

```typescript
interface Alarm {
  /** 报警ID */
  id?: string;
  /** 调度任务ID */
  scheduleTaskId?: string;
  /** 用户ID（接收报警的用户） */
  userId: number;
  /** 报警类型 */
  type: AlarmType;
}

enum AlarmType {
  /** 短信报警 */
  SMS = 'SMS',
  /** 邮件报警 */
  SMTP = 'SMTP'
}
```

### 12. 看门狗配置 WatchDog

```typescript
interface WatchDog {
  /** 是否启用看门狗 */
  watchEnabled: boolean;
  /** 检查间隔（秒） */
  watchIntervalSec: number;
  /** 超时时间（秒） */
  watchTimeoutSec: number;
  /** 第一次成功后是否停止 */
  stopOnFirstSuccess: boolean;
}
```

### 13. 课表任务生成器 CourseScheduleTaskGenerator

```typescript
interface CourseScheduleTaskGenerator {
  /** 实验室ID列表 */
  laboratoryId: number[];
  /** 检查间隔（Cron表达式，默认每5分钟） */
  cron?: string;
  /** 提前执行时间（分钟，默认7分钟） */
  earlyStart?: number;
  /** 延迟结束时间（分钟，默认7分钟） */
  delayEnd?: number;
  /** 是否启用（默认true） */
  enable?: boolean;
}
```

---

## API 接口

### 基础响应格式

```typescript
interface R<T> {
  code: number;
  message: string;
  data: T;
  success: boolean;
}

interface DiyResponseEntity<T> {
  body: T;
  statusCode: number;
}
```

### 1. 创建定时任务

**接口：** `POST /quartz/create`

**请求体：**
```typescript
// ScheduleConfigRoot
{
  task: {
    id: string;           // 必填，建议用雪花ID
    taskName: string;     // 必填
    cron: string;         // 必填
    enable: boolean;      // 可选，默认true
    startDate: string;    // 必填，格式：YYYY-MM-DD
    endDate: string;      // 必填，格式：YYYY-MM-DD
    laboratoryId: number; // 必填
  },
  actionGroups: [...],
  dataGroup: [...],
  conditionGroups: [...],
  timeRule: {...},
  alarmGroup: [...],
  watchDog: {...}
}
```

**响应：** `DiyResponseEntity<R<string>>`

---

### 2. 更新定时任务

**接口：** `PUT /quartz/update`

**请求体：** 同创建（ScheduleConfigRoot）

**说明：** 更新采用"删除旧数据+插入新数据"策略，前端提交完整配置即可。

**响应：** `DiyResponseEntity<R<boolean>>`

---

### 3. 删除定时任务

**接口：** `DELETE /quartz/delete`

**参数：**
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| taskId | string | 是 | 任务ID |

**响应：** `DiyResponseEntity<R<string>>`

---

### 4. 取消/禁用定时任务

**接口：** `POST /quartz/cancel`

**参数：**
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| taskId | string | 是 | 任务ID |

**说明：** 只是禁用任务，不从数据库删除。

**响应：** `DiyResponseEntity<R<string>>`

---

### 5. 启用定时任务

**接口：** `POST /quartz/enable`

**参数：**
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| taskId | string | 是 | 任务ID |

**响应：** `DiyResponseEntity<R<string>>`

---

### 6. 获取定时任务列表

**接口：** `GET /quartz/list`

**参数：**
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| enable | boolean | 否 | 按启用状态筛选 |

**响应：** `DiyResponseEntity<R<ScheduleTask[]>>`

---

### 7. 按实验室查询任务（分页）

**接口：** `GET /quartz/list-by-lab`

**参数：**
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| laboratoryId | number | 是 | 实验室ID |
| enable | boolean | 否 | 启用状态筛选 |
| deviceType | DeviceType | 否 | 设备类型筛选 |
| deviceId | number | 否 | 设备ID筛选 |
| commandLine | CommandLine | 否 | 指令筛选 |
| pageNum | number | 否 | 页码，默认1 |
| pageSize | number | 否 | 每页大小，默认20，最大100 |

**响应：** `DiyResponseEntity<R<Page<ScheduleConfigRoot>>>`

```typescript
interface Page<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
```

---

### 8. 从课表生成定时任务

**接口：** `POST /quartz/generate-from-course-schedule`

**请求体：**
```typescript
{
  laboratoryId: number[];  // 必填，实验室ID列表
  cron?: string;           // 可选，默认"0 0/5 * * * ?"
  earlyStart?: number;     // 可选，提前执行分钟数，默认7
  delayEnd?: number;       // 可选，延迟结束分钟数，默认7
  enable?: boolean;        // 可选，默认true
}
```

**响应：** `DiyResponseEntity<R<boolean>>`

**业务说明：**
1. 根据实验室ID查询关联的课表
2. 为每个课表生成一个定时任务
3. 任务名称格式：`{课程名}-实验室{labId}-第{startWeek}-{endWeek}周-第{startSection}-{endSection}节-{星期}`
4. 时间规则会根据 earlyStart/delayEnd 自动调整

---

## 数据验证规则

### 创建/更新时的验证

1. **任务主体验证**
   - id: 不能为空
   - taskName: 不能为空
   - cron: 不能为空
   - startDate/endDate: 不能为空，且开始日期不能晚于结束日期
   - laboratoryId: 不能为空

2. **动作组验证**
   - 每个 Action 必须有：deviceType, deviceId, commandLine, args
   - deviceId 对应的设备必须存在
   - actionGroupId 必须正确关联

3. **条件组验证**
   - 每个 Condition 必须有：expr（SpEL表达式）
   - 表达式格式必须正确：`#{data.{id}}.property operator value`
   - 表达式引用的 dataId 必须在 dataGroup 中存在
   - 引用的属性必须在对应设备记录中存在

4. **数据源验证**
   - 每个 Data 必须有：deviceId, deviceType
   - 设备必须存在且可访问

5. **时间规则验证**
   - startWeek/endWeek: 必须大于0，且结束周 >= 开始周
   - weekdays: 不能为空，值为1-7
   - startTime/endTime: 不能为空

6. **报警配置验证**
   - userId: 必须存在且当前用户有权限管理
   - type: SMS要求用户有手机号，SMTP要求用户有邮箱

7. **看门狗验证**（如果启用）
   - watchIntervalSec: 必须大于0
   - watchTimeoutSec: 必须大于0

---

## 数据库表结构

### 核心表关系

```
schedule_task (主表)
    ├── condition_group (1:N) ── condition (1:N)
    ├── action_group (1:N) ── action (1:N)
    ├── data (1:N)
    ├── time_rule (1:1)
    └── alarm (1:N)
```

### 表字段详情

**schedule_task** - 任务主表
| 字段 | 类型 | 说明 |
|-----|------|------|
| id | VARCHAR(64) | 主键 |
| task_name | VARCHAR(128) | 任务名称 |
| cron | VARCHAR(64) | Cron表达式 |
| enable | VARCHAR(8) | 是否启用 |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期 |
| laboratory_id | BIGINT | 实验室ID（外键） |

**condition_group** - 条件组表
| 字段 | 类型 | 说明 |
|-----|------|------|
| id | VARCHAR(64) | 主键 |
| type | VARCHAR(16) | 类型：ALL/ANY |
| schedule_task_id | VARCHAR(64) | 任务ID（外键） |

**condition** - 条件表
| 字段 | 类型 | 说明 |
|-----|------|------|
| id | VARCHAR(64) | 主键 |
| expr | VARCHAR(512) | SpEL表达式 |
| desc | VARCHAR(255) | 描述 |
| condition_group_id | VARCHAR(64) | 条件组ID（外键） |
| schedule_task_id | VARCHAR(64) | 任务ID（外键） |

**action_group** - 动作组表
| 字段 | 类型 | 说明 |
|-----|------|------|
| id | VARCHAR(64) | 主键 |
| schedule_task_id | VARCHAR(64) | 任务ID（外键） |
| condition_group_id | VARCHAR(64) | 关联条件组ID（外键） |

**action** - 动作表
| 字段 | 类型 | 说明 |
|-----|------|------|
| id | VARCHAR(64) | 主键 |
| device_type | VARCHAR(32) | 设备类型 |
| device_id | BIGINT | 设备ID |
| command_line | VARCHAR(64) | 指令 |
| args | JSON | 参数数组 |
| action_group_id | VARCHAR(64) | 动作组ID（外键） |
| schedule_task_id | VARCHAR(64) | 任务ID（外键） |

**data** - 数据源表
| 字段 | 类型 | 说明 |
|-----|------|------|
| id | VARCHAR(64) | 主键 |
| schedule_task_id | VARCHAR(64) | 任务ID（外键） |
| device_id | BIGINT | 设备ID |
| device_type | VARCHAR(32) | 设备类型 |

**time_rule** - 时间规则表
| 字段 | 类型 | 说明 |
|-----|------|------|
| id | VARCHAR(64) | 主键 |
| schedule_task_id | VARCHAR(64) | 任务ID（外键，唯一） |
| semester_id | BIGINT | 学期ID |
| weekdays | JSON | 星期列表 |
| start_week | INT | 开始周 |
| end_week | INT | 结束周 |
| week_type | VARCHAR(16) | 单双周类型 |
| start_time | TIME | 开始时间 |
| end_time | TIME | 结束时间 |

**alarm** - 报警配置表
| 字段 | 类型 | 说明 |
|-----|------|------|
| id | VARCHAR(64) | 主键 |
| schedule_task_id | VARCHAR(64) | 任务ID（外键） |
| user_id | BIGINT | 用户ID |
| type | VARCHAR(16) | 报警类型：SMS/SMTP |

---

## 前端组件设计建议

### 1. 表单结构

建议采用**步骤条 + 标签页**的混合布局：

```
步骤1: 基础配置
  ├── 任务名称
  ├── Cron表达式（提供常用选择器）
  ├── 生效日期范围
  └── 所属实验室

步骤2: 数据源配置
  └── 添加数据源（设备选择器）

步骤3: 条件与动作
  ├── 条件组列表（可增删改）
  │   └── 条件表达式构建器
  └── 动作组列表
      └── 动作配置（设备+指令+参数）

步骤4: 高级配置
  ├── 时间规则（可选，默认用任务日期）
  ├── 报警配置
  └── 看门狗配置
```

### 2. 条件表达式构建器

提供可视化方式构建 SpEL 表达式：

```
[选择数据源] [选择属性] [操作符] [输入值]
     ↓           ↓         ↓        ↓
   下拉选择    下拉选择   下拉选择   输入框
   (data.id)  (属性列表)  (> >=    (数值/布尔)
                         < <=
                         == !=)
```

### 3. 动作配置表单

```
动作配置:
├── 设备类型 [下拉: 空调/照明/门禁/断路器/传感器]
├── 设备选择 [级联下拉，根据类型过滤]
├── 指令选择 [根据设备类型动态加载可用指令]
└── 参数配置 [根据指令动态渲染]
    └── 如空调增强控制：显示开关/模式/温度/风速四个字段
```

### 4. 课表任务生成向导

专门的弹窗/页面：

```
课表任务生成:
├── 实验室选择 [多选框]
├── Cron表达式 [默认0 0/5 * * * ?]
├── 提前执行 [分钟，默认7]
├── 延迟结束 [分钟，默认7]
└── [生成预览] [确认生成]
```

### 5. 任务列表展示

表格列设计：
| 列名 | 说明 |
|-----|------|
| 任务名称 | 可点击展开详情 |
| Cron | 显示为可读格式（如"每5分钟"） |
| 实验室 | 显示实验室名称 |
| 启用状态 | Switch开关 |
| 有效期 | 开始~结束日期 |
| 条件/动作数 | 统计信息 |
| 操作 | 编辑/删除/启用/禁用/执行日志 |

---

## 注意事项

1. **ID生成**：任务ID使用雪花算法（Snowflake）生成字符串，前端创建时可调用后端接口获取或使用 UUID。

2. **级联删除**：数据库设置了 CASCADE，删除任务会自动删除关联的条件、动作等子数据。

3. **条件执行逻辑**：
   - 动作组通过 `conditionGroupId` 关联条件组
   - 条件组类型为 ALL：所有条件满足才执行
   - 条件组类型为 ANY：任一条件满足即执行
   - 多个动作组之间是独立判断的

4. **看门狗机制**：
   - 启用后任务会周期性执行直到成功或超时
   - `stopOnFirstSuccess=true`：第一次成功就停止
   - `watchTimeoutSec`：超时后强制停止

5. **时间规则双重校验**：
   - Cron 控制任务何时被触发
   - TimeRule 在任务执行时校验当前是否在有效时间窗口内

6. **报警触发场景**：
   - 条件触发：时间规则异常、数据源异常、条件表达式评估失败
   - 设备异常：设备离线、数据获取失败
