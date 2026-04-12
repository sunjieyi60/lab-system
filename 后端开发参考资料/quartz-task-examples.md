# Quartz 定时任务配置示例

本文档提供适用于 `/quartz/create` 接口的空调设备条件控制任务 JSON 配置示例。

**实际设备数据参考**：
```
recordId: 8618-8614, laboratoryId: 200, address: 31, selfId: 1
airConditionDeviceId: 500
roomTemperature: 17℃, temperature: 17-26℃, errorCode: 0
isOpen: 0(关闭)/1(开启), mode: 制冷, speed: 自动/中速
```

**学期设置**：
```
semesterId: 710, name: "2025-2026 第0学年"
startDate: 2026-02-24, endDate: 2026-03-15
```

---

## 字段说明

### ScheduleTask（任务主体）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 任务唯一标识（雪花ID） |
| taskName | String | 任务名称，用于展示 |
| cron | String | Quartz Cron 表达式，定义触发周期 |
| enable | Boolean | 是否启用：true=启用，false=禁用 |
| startDate | LocalDate | 任务生效开始日期（yyyy-MM-dd） |
| endDate | LocalDate | 任务生效结束日期（yyyy-MM-dd） |
| laboratoryId | Long | 所属实验室ID（实际数据：200） |

### Action（动作）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 动作唯一标识（雪花ID） |
| deviceType | String | 设备类型：AirCondition/Light/Access/Sensor/CircuitBreak |
| deviceId | Long | 设备ID（需查询获取） |
| commandLine | String | 指令枚举值，见下方指令说明 |
| args | Integer[] | 指令参数数组，格式依指令而定 |
| actionGroupId | String | 所属动作组ID |
| scheduleTaskId | String | 所属任务ID |

### 指令参数说明

**ENHANCE_CONTROL_AIR_CONDITION（增强控制）**
```
args[0] = address      // 设备地址（实际数据：31）
args[1] = selfId       // 设备子ID（实际数据：1）
args[2] = 开关         // 0=关, 1=开
args[3] = 模式         // 1=制热, 2=制冷, 4=送风, 8=除湿
args[4] = 温度         // 16-30（摄氏度，实际数据：17-26）
args[5] = 风速         // 0=自动, 1=低, 2=中, 3=高（实际：中速=2, 自动=0）
```

**OPEN_AIR_CONDITION_RS485 / CLOSE_AIR_CONDITION_RS485（开关）**
```
args[0] = address      // 设备地址（实际数据：31）
args[1] = selfId       // 设备子ID（实际数据：1）
```

**REQUEST_AIR_CONDITION_DATA_RS485（请求数据）**
```
args[0] = address      // 设备地址（实际数据：31）
args[1] = selfId       // 设备子ID（实际数据：1）
```

### Condition（条件）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 条件唯一标识（雪花ID） |
| expr | String | SpEL 表达式，支持 #{dataId}.属性 引用数据，如 #{1789569705678901234}.roomTemperature |
| desc | String | 条件描述说明 |
| conditionGroupId | String | 所属条件组ID |
| scheduleTaskId | String | 所属任务ID |

### SpEL 表达式可用字段（基于 AirConditionRecord）
| 字段 | 类型 | 示例值 | 说明 |
|------|------|--------|------|
| address | Integer | 31 | 设备地址 |
| selfId | Integer | 1 | 设备子ID |
| isOpen | Boolean | true/false | 是否开启 |
| mode | String | "制冷" | 工作模式 |
| temperature | Integer | 17-26 | 设定温度 |
| speed | String | "自动"/"中速" | 风速 |
| roomTemperature | Integer | 17 | 房间实际温度 |
| errorCode | Integer | 0 | 0=正常,1=故障,2=通信失败 |

### ConditionGroup（条件组）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 条件组唯一标识（雪花ID） |
| type | String | 条件组合方式：ALL=全部满足，ANY=任一满足 |
| scheduleTaskId | String | 所属任务ID |
| conditions | List | 条件列表 |

### TimeRule（时间规则）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 规则唯一标识（雪花ID） |
| scheduleTaskId | String | 所属任务ID |
| semesterId | Long | 学期ID（实际：710） |
| weekdays | List<Integer> | 生效星期：[1,2,3,4,5] 表示周一至周五 |
| startWeek | Integer | 开始周次 |
| endWeek | Integer | 结束周次 |
| weekType | String | 单双周：ALL=全部，ODD=单周，EVEN=双周 |
| startTime | LocalTime | 日内开始时间（HH:mm:ss） |
| endTime | LocalTime | 日内结束时间（HH:mm:ss） |

### WatchDog（看门狗）
| 字段 | 类型 | 说明 |
|------|------|------|
| watchEnabled | Boolean | 是否启用看门狗监控 |
| watchIntervalSec | Integer | 监控间隔（秒） |
| watchTimeoutSec | Integer | 超时时间（秒） |
| stopOnFirstSuccess | Boolean | 首次成功是否停止监控 |

### Alarm（报警配置）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 报警配置唯一标识（雪花ID） |
| scheduleTaskId | String | 所属任务ID |
| userId | Long | 接收报警的用户ID |
| type | String | 报警类型：SMS=短信，SMTP=邮件 |

---

## 示例 1：房间温度低时自动关闭空调

**场景**：每5分钟检测一次，当房间温度 roomTemperature ≤ 18℃ 时自动关闭空调（根据实际数据，房间温度17℃）

```json
{
  // ==================== 任务主体 ====================
  "task": {
    "id": "1789569701234567890",          // 雪花ID：任务唯一标识
    "taskName": "低温自动关闭空调",        // 任务名称
    "cron": "0 */5 * * * ?",               // 每5分钟执行一次
    "enable": true,                        // 启用状态
    "startDate": "2026-02-24",             // 生效开始日期（学期开始日期）
    "endDate": "2026-03-15",               // 生效结束日期（学期结束日期）
    "laboratoryId": 200                    // 实验室ID（实际数据）
  },
  
  // ==================== 动作组 ====================
  "actionGroups": [
    {
      "id": "1789569702345678901",         // 雪花ID：动作组唯一标识
      "scheduleTaskId": "1789569701234567890",  // 关联任务ID
      "conditionGroupId": "1789569704567890123", // 关联条件组ID
      "actions": [
        {
          "id": "1789569703456789012",     // 雪花ID：动作唯一标识
          "deviceType": "AirCondition",    // 设备类型：空调
          "deviceId": 500,                 // 空调设备ID（实际数据）
          "commandLine": "CLOSE_AIR_CONDITION_RS485",  // 关闭空调指令
          "args": [31, 1],                 // [地址31,子ID1]（实际数据）
          "actionGroupId": "1789569702345678901",  // 所属动作组
          "scheduleTaskId": "1789569701234567890"  // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 数据组 ====================
  "dataGroup": [
    {
      "id": "1789569705678901234",         // 雪花ID：数据定义唯一标识
      "scheduleTaskId": "1789569701234567890",  // 关联任务ID
      "deviceId": 500,                   // 数据来源设备ID（实际数据）
      "deviceType": "AirCondition"       // 设备类型
    }
  ],
  
  // ==================== 条件组 ====================
  "conditionGroups": [
    {
      "id": "1789569704567890123",         // 雪花ID：条件组唯一标识
      "type": "ALL",                       // 条件类型：全部满足
      "scheduleTaskId": "1789569701234567890",  // 关联任务ID
      "conditions": [
        {
          "id": "1789569706789012345",     // 雪花ID：条件唯一标识
          "expr": "#{1789569705678901234}.roomTemperature <= 18",  // SpEL：房间温度≤18度
          "desc": "房间温度低于或等于18度，需要关闭空调",  // 条件描述
          "conditionGroupId": "1789569704567890123",  // 所属条件组
          "scheduleTaskId": "1789569701234567890"     // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569707890123456",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569701234567890",    // 关联任务ID
    "weekdays": [1, 2, 3, 4, 5],           // 周一至周五
    "startWeek": 1,                        // 第1周开始
    "endWeek": 20,                         // 第20周结束
    "weekType": "ALL",                     // 全部周次
    "startTime": "08:00:00",               // 每天8点开始生效
    "endTime": "18:00:00"                  // 每天18点结束生效
  },
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569707890123456",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569701234567890",    // 关联任务ID
    "semesterId": 710,                     // 学期ID（实际数据：2025-2026 第0学年）
    "weekdays": [1, 2, 3, 4, 5],           // 周一至周五
    "startWeek": 1,                        // 第1周开始
    "endWeek": 3,                          // 第3周结束（学期仅约3周）
    "weekType": "ALL",                     // 全部周次
    "startTime": "08:00:00",               // 每天8点开始生效
    "endTime": "18:00:00"                  // 每天18点结束生效
  },
  
  // ==================== 报警配置 ====================
  "alarmGroup": [
    {
      "id": "1789569708901234567",         // 雪花ID：报警配置唯一标识
      "scheduleTaskId": "1789569701234567890",    // 关联任务ID
      "userId": 10001,                     // 接收报警用户ID
      "type": "SMTP"                       // 报警方式：邮件
    }
  ],
  
  // ==================== 看门狗配置 ====================
  "watchDog": {
    "watchEnabled": true,                  // 启用看门狗
    "watchIntervalSec": 30,                // 30秒检查一次
    "watchTimeoutSec": 300,                // 5分钟超时
    "stopOnFirstSuccess": false            // 不自动停止，持续监控
  }
}
```

---

## 示例 2：房间温度高时自动开启制冷

**场景**：每5分钟检测一次，当房间温度 roomTemperature ≥ 25℃ 时，自动开启空调制冷到24℃

```json
{
  // ==================== 任务主体 ====================
  "task": {
    "id": "1789569711234567890",          // 雪花ID：任务唯一标识
    "taskName": "高温自动开启制冷",        // 任务名称
    "cron": "0 */5 * * * ?",               // 每5分钟执行一次
    "enable": true,                        // 启用状态
    "startDate": "2026-02-24",             // 生效开始日期（学期开始日期）
    "endDate": "2026-03-15",               // 生效结束日期（学期结束日期）
    "laboratoryId": 200                    // 实验室ID（实际数据）
  },
  
  // ==================== 动作组 ====================
  "actionGroups": [
    {
      "id": "1789569712345678901",         // 雪花ID：动作组唯一标识
      "scheduleTaskId": "1789569711234567890",  // 关联任务ID
      "conditionGroupId": "1789569714567890123", // 关联条件组ID
      "actions": [
        {
          "id": "1789569713456789012",     // 雪花ID：动作唯一标识
          "deviceType": "AirCondition",    // 设备类型：空调
          "deviceId": 500,                 // 空调设备ID（实际数据）
          "commandLine": "ENHANCE_CONTROL_AIR_CONDITION",  // 增强控制指令
          "args": [31, 1, 1, 2, 24, 0],    // [地址31,子ID1,开机,制冷,24度,自动风]（实际数据）
          "actionGroupId": "1789569712345678901",  // 所属动作组
          "scheduleTaskId": "1789569711234567890"  // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 数据组 ====================
  "dataGroup": [
    {
      "id": "1789569715678901234",         // 雪花ID：数据定义唯一标识
      "scheduleTaskId": "1789569711234567890",  // 关联任务ID
      "deviceId": 500,                   // 数据来源设备ID（实际数据）
      "deviceType": "AirCondition"       // 设备类型
    }
  ],
  
  // ==================== 条件组 ====================
  "conditionGroups": [
    {
      "id": "1789569714567890123",         // 雪花ID：条件组唯一标识
      "type": "ALL",                       // 条件类型：全部满足
      "scheduleTaskId": "1789569711234567890",  // 关联任务ID
      "conditions": [
        {
          "id": "1789569716789012345",     // 雪花ID：条件唯一标识
          "expr": "#{1789569715678901234}.roomTemperature >= 25",  // SpEL：房间温度≥25度
          "desc": "房间温度过高，需要开启制冷",  // 条件描述
          "conditionGroupId": "1789569714567890123",  // 所属条件组
          "scheduleTaskId": "1789569711234567890"     // 所属任务
        }
      ]
    }
  ],
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569707890123456",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569701234567890",    // 关联任务ID
    "semesterId": 710,                     // 学期ID（实际数据：2025-2026 第0学年）
    "weekdays": [1, 2, 3, 4, 5],           // 周一至周五
    "startWeek": 1,                        // 第1周开始
    "endWeek": 3,                          // 第3周结束（学期仅约3周）
    "weekType": "ALL",                     // 全部周次
    "startTime": "08:00:00",               // 每天8点开始生效
    "endTime": "18:00:00"                  // 每天18点结束生效
  },
  
  // ==================== 报警配置 ====================
  "alarmGroup": [
  ],
  
  // ==================== 看门狗配置 ====================
  "watchDog": {
    "watchEnabled": true,                  // 启用看门狗
    "watchIntervalSec": 30,                // 30秒检查一次
    "watchTimeoutSec": 300,                // 5分钟超时
    "stopOnFirstSuccess": false            // 不自动停止，持续监控
  }
}
```

---

## 示例 3：空调故障自动报警

**场景**：每10分钟检测一次，当空调 errorCode > 0 时发送短信和邮件报警（实际数据：errorCode=0 表示正常）

```json
{
  // ==================== 任务主体 ====================
  "task": {
    "id": "1789569721234567890",          // 雪花ID：任务唯一标识
    "taskName": "空调故障监控报警",        // 任务名称
    "cron": "0 */10 * * * ?",              // 每10分钟执行一次
    "enable": true,                        // 启用状态
    "startDate": "2026-02-24",             // 生效开始日期（学期开始日期）
    "endDate": "2026-03-15",               // 生效结束日期（学期结束日期）
    "laboratoryId": 200                    // 实验室ID（实际数据）
  },
  
  // ==================== 动作组 ====================
  "actionGroups": [
    {
      "id": "1789569722345678901",         // 雪花ID：动作组唯一标识
      "scheduleTaskId": "1789569721234567890",  // 关联任务ID
      "conditionGroupId": "1789569724567890123", // 关联条件组ID
      "actions": [
        {
          "id": "1789569723456789012",     // 雪花ID：动作唯一标识
          "deviceType": "AirCondition",    // 设备类型：空调
          "deviceId": 500,                 // 空调设备ID（实际数据）
          "commandLine": "REQUEST_AIR_CONDITION_DATA_RS485",  // 请求数据指令
          "args": [31, 1],                 // [地址31,子ID1]（实际数据）
          "actionGroupId": "1789569722345678901",  // 所属动作组
          "scheduleTaskId": "1789569721234567890"  // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 数据组 ====================
  "dataGroup": [
    {
      "id": "1789569725678901234",         // 雪花ID：数据定义唯一标识
      "scheduleTaskId": "1789569721234567890",  // 关联任务ID
      "deviceId": 500,                   // 数据来源设备ID（实际数据）
      "deviceType": "AirCondition"       // 设备类型
    }
  ],
  
  // ==================== 条件组 ====================
  "conditionGroups": [
    {
      "id": "1789569724567890123",         // 雪花ID：条件组唯一标识
      "type": "ANY",                       // 条件类型：任一满足
      "scheduleTaskId": "1789569721234567890",  // 关联任务ID
      "conditions": [
        {
          "id": "1789569726789012345",     // 雪花ID：条件唯一标识
          "expr": "#{1789569725678901234}.errorCode > 0",  // SpEL：错误码>0表示故障
          "desc": "空调存在故障(1=设备故障,2=通信失败)",  // 条件描述
          "conditionGroupId": "1789569724567890123",  // 所属条件组
          "scheduleTaskId": "1789569721234567890"     // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569727890123456",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569721234567890",    // 关联任务ID
    "weekdays": [1, 2, 3, 4, 5, 6, 7],     // 每天
    "startWeek": 1,                        // 第1周开始
    "endWeek": 25,                         // 第25周结束
    "weekType": "ALL"                      // 全部周次（全天24小时）
  },
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569727890123456",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569721234567890",    // 关联任务ID
    "semesterId": 710,                     // 学期ID（实际数据：2025-2026 第0学年）
    "weekdays": [1, 2, 3, 4, 5, 6, 7],     // 每天
    "startWeek": 1,                        // 第1周开始
    "endWeek": 3,                          // 第3周结束（学期仅约3周）
    "weekType": "ALL"                      // 全部周次（全天24小时）
  },
  
  // ==================== 报警配置 ====================
  "alarmGroup": [
    {
      "id": "1789569728901234567",         // 雪花ID：报警配置唯一标识
      "scheduleTaskId": "1789569721234567890",    // 关联任务ID
      "userId": 10001,                     // 接收报警用户ID
      "type": "SMS"                        // 报警方式：短信
    },
    {
      "id": "1789569729012345678",         // 雪花ID：报警配置唯一标识
      "scheduleTaskId": "1789569721234567890",    // 关联任务ID
      "userId": 10001,                     // 接收报警用户ID
      "type": "SMTP"                       // 报警方式：邮件
    }
  ],
  
  // ==================== 看门狗配置 ====================
  "watchDog": {
    "watchEnabled": true,                  // 启用看门狗
    "watchIntervalSec": 60,                // 60秒检查一次
    "watchTimeoutSec": 600,                // 10分钟超时
    "stopOnFirstSuccess": true             // 首次成功后停止
  }
}
```

---

## 示例 4：定时开关空调（无条件纯定时）

**场景**：每天早8点开启空调（制冷17℃，中速），晚18点关闭（纯定时任务，无需条件判断）

### 4.1 早上8点开启空调

```json
{
  // ==================== 任务主体 ====================
  "task": {
    "id": "1789569731234567890",          // 雪花ID：任务唯一标识
    "taskName": "空调定时开启",            // 任务名称
    "cron": "0 0 8 * * ?",                 // 每天8点执行
    "enable": true,                        // 启用状态
    "startDate": "2026-02-24",             // 生效开始日期（学期开始日期）
    "endDate": "2026-03-15",               // 生效结束日期（学期结束日期）
    "laboratoryId": 200                    // 实验室ID（实际数据）
  },
  
  // ==================== 动作组 ====================
  "actionGroups": [
    {
      "id": "1789569732345678901",         // 雪花ID：动作组唯一标识
      "scheduleTaskId": "1789569731234567890",  // 关联任务ID
      "actions": [
        {
          "id": "1789569733456789012",     // 雪花ID：动作唯一标识
          "deviceType": "AirCondition",    // 设备类型：空调
          "deviceId": 500,                 // 空调设备ID（实际数据）
          "commandLine": "ENHANCE_CONTROL_AIR_CONDITION",  // 增强控制指令
          "args": [31, 1, 1, 2, 17, 2],    // [地址31,子ID1,开机,制冷,17度,中风]（实际数据）
          "actionGroupId": "1789569732345678901",  // 所属动作组
          "scheduleTaskId": "1789569731234567890"  // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 数据组（无条件时可为空） ====================
  "dataGroup": [],
  
  // ==================== 条件组（无条件时可为空） ====================
  "conditionGroups": [],
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569735678901234",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569731234567890",    // 关联任务ID
    "weekdays": [1, 2, 3, 4, 5],           // 周一至周五
    "startWeek": 1,                        // 第1周开始
    "endWeek": 20,                         // 第20周结束
    "weekType": "ALL"                      // 全部周次
  },
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569735678901234",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569731234567890",    // 关联任务ID
    "semesterId": 710,                     // 学期ID（实际数据：2025-2026 第0学年）
    "weekdays": [1, 2, 3, 4, 5],           // 周一至周五
    "startWeek": 1,                        // 第1周开始
    "endWeek": 3,                          // 第3周结束（学期仅约3周）
    "weekType": "ALL"                      // 全部周次
  },
  
  // ==================== 报警配置（可选） ====================
  "alarmGroup": [],
  
  // ==================== 看门狗配置 ====================
  "watchDog": {
    "watchEnabled": false,                 // 禁用看门狗
    "watchIntervalSec": 30,
    "watchTimeoutSec": 300,
    "stopOnFirstSuccess": false
  }
}
```

### 4.2 晚上18点关闭空调

```json
{
  // ==================== 任务主体 ====================
  "task": {
    "id": "1789569741234567890",          // 雪花ID：任务唯一标识
    "taskName": "空调定时关闭",            // 任务名称
    "cron": "0 0 18 * * ?",                // 每天18点执行
    "enable": true,                        // 启用状态
    "startDate": "2026-02-24",             // 生效开始日期（学期开始日期）
    "endDate": "2026-03-15",               // 生效结束日期（学期结束日期）
    "laboratoryId": 200                    // 实验室ID（实际数据）
  },
  
  // ==================== 动作组 ====================
  "actionGroups": [
    {
      "id": "1789569742345678901",         // 雪花ID：动作组唯一标识
      "scheduleTaskId": "1789569741234567890",  // 关联任务ID
      "actions": [
        {
          "id": "1789569743456789012",     // 雪花ID：动作唯一标识
          "deviceType": "AirCondition",    // 设备类型：空调
          "deviceId": 500,                 // 空调设备ID（实际数据）
          "commandLine": "CLOSE_AIR_CONDITION_RS485",  // 关闭空调指令
          "args": [31, 1],                 // [地址31,子ID1]（实际数据）
          "actionGroupId": "1789569742345678901",  // 所属动作组
          "scheduleTaskId": "1789569741234567890"  // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 数据组（无条件时可为空） ====================
  "dataGroup": [],
  
  // ==================== 条件组（无条件时可为空） ====================
  "conditionGroups": [],
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569745678901234",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569741234567890",    // 关联任务ID
    "semesterId": 710,                     // 学期ID（实际数据：2025-2026 第0学年）
    "weekdays": [1, 2, 3, 4, 5],           // 周一至周五
    "startWeek": 1,                        // 第1周开始
    "endWeek": 3,                          // 第3周结束（学期仅约3周）
    "weekType": "ALL"                      // 全部周次
  },
  
  // ==================== 报警配置（可选） ====================
  "alarmGroup": [],
  
  // ==================== 看门狗配置 ====================
  "watchDog": {
    "watchEnabled": false,                 // 禁用看门狗
    "watchIntervalSec": 30,
    "watchTimeoutSec": 300,
    "stopOnFirstSuccess": false
  }
}
```

---

## 示例 5：多条件组合控制（智能温控）

**场景**：工作日上班时间，当 roomTemperature > 22℃ 且 isOpen == false（空调未开启）时，自动开启制冷

```json
{
  // ==================== 任务主体 ====================
  "task": {
    "id": "1789569751234567890",          // 雪花ID：任务唯一标识
    "taskName": "智能温控-未开空调",       // 任务名称
    "cron": "0 */3 * * * ?",               // 每3分钟执行一次
    "enable": true,                        // 启用状态
    "startDate": "2026-02-24",             // 生效开始日期（学期开始日期）
    "endDate": "2026-03-15",               // 生效结束日期（学期结束日期）
    "laboratoryId": 200                    // 实验室ID（实际数据）
  },
  
  // ==================== 动作组 ====================
  "actionGroups": [
    {
      "id": "1789569752345678901",         // 雪花ID：动作组唯一标识
      "scheduleTaskId": "1789569751234567890",  // 关联任务ID
      "conditionGroupId": "1789569754567890123", // 关联条件组ID
      "actions": [
        {
          "id": "1789569753456789012",     // 雪花ID：动作唯一标识
          "deviceType": "AirCondition",    // 设备类型：空调
          "deviceId": 500,                 // 空调设备ID（实际数据）
          "commandLine": "ENHANCE_CONTROL_AIR_CONDITION",  // 增强控制指令
          "args": [31, 1, 1, 2, 26, 2],    // [地址31,子ID1,开机,制冷,26度,中风]（实际数据）
          "actionGroupId": "1789569752345678901",  // 所属动作组
          "scheduleTaskId": "1789569751234567890"  // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 数据组 ====================
  "dataGroup": [
    {
      "id": "1789569755678901234",         // 雪花ID：数据定义唯一标识
      "scheduleTaskId": "1789569751234567890",  // 关联任务ID
      "deviceId": 500,                   // 数据来源设备ID（实际数据）
      "deviceType": "AirCondition"       // 设备类型
    }
  ],
  
  // ==================== 条件组（多条件组合） ====================
  "conditionGroups": [
    {
      "id": "1789569754567890123",         // 雪花ID：条件组唯一标识
      "type": "ALL",                       // 条件类型：全部满足
      "scheduleTaskId": "1789569751234567890",  // 关联任务ID
      "conditions": [
        {
          "id": "1789569756789012345",     // 雪花ID：条件唯一标识
          "expr": "#{1789569755678901234}.roomTemperature > 22",
          "desc": "房间温度超过22度",        // 条件描述
          "conditionGroupId": "1789569754567890123",  // 所属条件组
          "scheduleTaskId": "1789569751234567890"     // 所属任务
        },
        {
          "id": "1789569757890123456",     // 雪花ID：条件唯一标识
          "expr": "#{1789569755678901234}.isOpen == false",
          "desc": "空调当前处于关闭状态",     // 条件描述（实际数据中isOpen=0表示关闭）
          "conditionGroupId": "1789569754567890123",  // 所属条件组
          "scheduleTaskId": "1789569751234567890"     // 所属任务
        }
      ]
    }
  ],
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569758901234567",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569751234567890",    // 关联任务ID
    "weekdays": [1, 2, 3, 4, 5],           // 周一至周五
    "startWeek": 1,                        // 第1周开始
    "endWeek": 20,                         // 第20周结束
    "weekType": "ALL",                     // 全部周次
    "startTime": "09:00:00",               // 每天9点开始生效
    "endTime": "17:30:00"                  // 每天17:30结束生效
  },
  
  // ==================== 时间规则 ====================
  "timeRule": {
    "id": "1789569758901234567",           // 雪花ID：时间规则唯一标识
    "scheduleTaskId": "1789569751234567890",    // 关联任务ID
    "semesterId": 710,                     // 学期ID（实际数据：2025-2026 第0学年）
    "weekdays": [1, 2, 3, 4, 5],           // 周一至周五
    "startWeek": 1,                        // 第1周开始
    "endWeek": 3,                          // 第3周结束（学期仅约3周）
    "weekType": "ALL",                     // 全部周次
    "startTime": "09:00:00",               // 每天9点开始生效
    "endTime": "17:30:00"                  // 每天17:30结束生效
  },
  
  // ==================== 报警配置 ====================
  "alarmGroup": [
    {
      "id": "1789569759012345678",         // 雪花ID：报警配置唯一标识
      "scheduleTaskId": "1789569751234567890",    // 关联任务ID
      "userId": 10001,                     // 接收报警用户ID
      "type": "SMTP"                       // 报警方式：邮件
    }
  ],
  
  // ==================== 看门狗配置 ====================
  "watchDog": {
    "watchEnabled": true,                  // 启用看门狗
    "watchIntervalSec": 30,                // 30秒检查一次
    "watchTimeoutSec": 180,                // 3分钟超时
    "stopOnFirstSuccess": true             // 首次成功后停止
  }
}
```

---

## 数据字段映射参考

根据实际空调记录数据，SpEL 表达式可用字段：

| 实际数据 | JSON字段名 | 数据类型 | 示例表达式 |
|----------|-----------|----------|-----------|
| 31 | address | Integer | `#{1789...}.address == 31` |
| 1 | selfId | Integer | `#{1789...}.selfId == 1` |
| 0/1 | isOpen | Boolean | `#{1789...}.isOpen == true` |
| 制冷 | mode | String | `#{1789...}.mode == '制冷'` |
| 17-26 | temperature | Integer | `#{1789...}.temperature >= 20` |
| 自动/中速 | speed | String | `#{1789...}.speed == '中速'` |
| 17 | roomTemperature | Integer | `#{1789...}.roomTemperature <= 18` |
| 0 | errorCode | Integer | `#{1789...}.errorCode > 0` |

---

## 使用说明

### 1. 生成雪花ID
前端可使用以下方式生成雪花ID（19位长整数）：

```javascript
// 使用雪花ID库（如 snowflake-id 或 @theinternetfolks/snowflake）
import { Snowflake } from 'snowflake-id';

const snowflake = new Snowflake({
  mid: 1,  // 机器ID
  offset: 0
});

const id = snowflake.generate();  // 生成 1789569701234567890 格式的ID
```

### 2. 调用接口

```bash
curl -X POST http://localhost:8088/quartz/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d @example1-low-temp-close.json
```

### 3. 验证任务

创建成功后，可通过以下接口查看任务列表：

```bash
# 查看所有任务
curl http://localhost:8088/quartz/list

# 查看启用的任务
curl http://localhost:8088/quartz/list?enable=true
```

---

## 注意事项

1. **ID 唯一性**：所有 ID 字段必须使用全局唯一的雪花ID，不能重复
2. **关联关系**：子组件的 `scheduleTaskId`、`actionGroupId`、`conditionGroupId` 等必须正确指向父组件
3. **SpEL 表达式**：条件表达式中的 `#{id}` 中的 `id` 必须是 `dataGroup` 中定义的 `id`
4. **设备参数**：`args` 参数必须严格按照协议规范填写，实际数据：address=31, selfId=1
5. **实际数据参考**：
   - laboratoryId = 200
   - airConditionDeviceId = 500
   - address = 31, selfId = 1
   - roomTemperature = 17℃
   - temperature = 17-26℃
   - mode = 制冷, speed = 自动/中速
   - isOpen = false(关闭)/true(开启)
   - errorCode = 0(正常)
6. **学期设置**：所有示例已配置 semesterId=710（2025-2026 第0学年，2026-02-24 至 2026-03-15），任务日期已调整为在学期范围内
