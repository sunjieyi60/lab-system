# Lab System 设备管理 CRUD Skill

## 概述

本文档描述了实验室综合管理系统中**设备管理模块**的完整数据模型、API 接口和业务规则，专注于设备的增删改查（CRUD）操作，用于构建前端 Vue + TypeScript 设备管理组件。

---

## ⚠️ 重要提示

```diff
- 🚧 Socket 网关功能尚未实现 - 仅提供接口定义，请勿在生产环境使用
- 🚧 SocketGateway 相关 API 目前为预留接口，后端逻辑未完成
```

**当前可用功能：**
- ✅ RS485 网关管理（完整实现）
- ✅ 设备管理（RS485 网关下的设备）
- ✅ 设备记录查询
- ✅ 轮询控制

**暂不可用功能：**
- ❌ Socket 网关创建/删除
- ❌ Socket 网关下的设备管理
- ❌ 空调设备的 Socket 网关选项

---

## 数据模型

### 1. 基础响应格式

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

### 2. 设备类型 DeviceType

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
| 设备类型 | 起始地址 | 结束地址 | 支持的网关 | 说明 |
|---------|---------|---------|-----------|------|
| AirCondition | 0x1F (31) | 0x28 (40) | RS485 / Socket(未实现) | 支持双网关，至少填一个 |
| CircuitBreak | 0x0B (11) | 0x1E (30) | RS485 | 电气监控 |
| Light | 0x29 (41) | 0x3C (60) | RS485 | 照明控制 |
| Sensor | 0x3D (61) | 0x50 (80) | RS485 | 环境监测 |
| Access | 0x01 (1) | 0x0A (10) | RS485 | 门禁控制 |

### 3. 基础设备 Device（基类）

```typescript
interface Device {
  /** 设备ID */
  id: number;
  /** 设备名称 */
  deviceName: string;
  /** 设备类型 */
  deviceType: DeviceType;
  /** 所属实验室ID */
  belongToLaboratoryId: number;
  /** 是否启用轮询检测（默认true） */
  pollingEnabled: boolean;
  /** 创建时间 */
  createTime?: string;
  /** 更新时间 */
  updateTime?: string;
}
```

### 4. 空调设备 AirCondition

```typescript
interface AirCondition extends Device {
  deviceType: DeviceType.AirCondition;
  /** 空调地址（RS485地址） */
  address: number;
  /** 地址下空调编号 */
  selfId: number;
  /** RS485网关ID（可选，与socketGatewayId至少填一个） */
  rs485GatewayId?: number;
  /** Socket网关ID（可选，与rs485GatewayId至少填一个） */
  socketGatewayId?: number;
  /** 机组ID（默认自动生成UUID） */
  groupId?: string;
  /** 是否锁定 */
  isLock?: boolean;
}
```

**创建空调设备 DTO (CreateAirCondition)：**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| deviceName | string | 是 | 设备名称 |
| deviceType | DeviceType | 是 | 固定为 AirCondition |
| belongToLaboratoryId | number | 是 | 所属实验室ID |
| groupId | string | 否 | 机组ID，用于多联机控制 |
| address | number | 是 | 空调地址（31-40） |
| selfId | number | 是 | 空调编号（地址下唯一） |
| rs485GatewayId | number | 条件 | RS485网关ID，与socketGatewayId至少填一个 |
| socketGatewayId | number | 条件 | Socket网关ID，与rs485GatewayId至少填一个 |

**验证规则：**
- 同一RS485网关下，address + selfId 组合必须唯一
- 同一Socket网关下，address + selfId 组合必须唯一
- 两个网关至少填写一个

### 5. 照明设备 Light

```typescript
interface Light extends Device {
  deviceType: DeviceType.Light;
  /** 灯光地址 */
  address: number;
  /** 地址下灯光编号 */
  selfId: number;
  /** RS485网关ID（必填） */
  rs485GatewayId: number;
  /** 是否锁定 */
  isLock?: boolean;
}
```

**创建设备 DTO (CreateLight)：**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| deviceName | string | 是 | 设备名称 |
| deviceType | DeviceType | 是 | 固定为 Light |
| belongToLaboratoryId | number | 是 | 所属实验室ID |
| groupId | string | 否 | 机组ID |
| address | number | 是 | 灯光地址（41-60） |
| selfId | number | 是 | 灯光编号 |
| rs485GatewayId | number | 是 | RS485网关ID |

**验证规则：**
- 同一RS485网关下，address + selfId 组合必须唯一

### 6. 门禁设备 Access

```typescript
interface Access extends Device {
  deviceType: DeviceType.Access;
  /** 门禁地址 */
  address: number;
  /** 门禁编号 */
  selfId: number;
  /** RS485网关ID（必填） */
  rs485GatewayId: number;
  /** 是否锁定 */
  isLock?: boolean;
}
```

**创建设备 DTO (CreateAccess)：**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| deviceName | string | 是 | 设备名称 |
| deviceType | DeviceType | 是 | 固定为 Access |
| belongToLaboratoryId | number | 是 | 所属实验室ID |
| groupId | string | 否 | 机组ID |
| address | number | 是 | 门禁地址（1-10） |
| selfId | number | 是 | 门禁编号 |
| rs485GatewayId | number | 是 | RS485网关ID |

### 7. 传感器设备 Sensor

```typescript
interface Sensor extends Device {
  deviceType: DeviceType.Sensor;
  /** 传感器地址 */
  address: number;
  /** 地址下传感器编号 */
  selfId: number;
  /** RS485网关ID（必填） */
  rs485GatewayId: number;
}
```

**创建设备 DTO (CreateSensor)：**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| deviceName | string | 是 | 设备名称 |
| deviceType | DeviceType | 是 | 固定为 Sensor |
| belongToLaboratoryId | number | 是 | 所属实验室ID |
| groupId | string | 否 | 机组ID |
| address | number | 是 | 传感器地址（61-80） |
| selfId | number | 是 | 传感器编号 |
| rs485GatewayId | number | 是 | RS485网关ID |

### 8. 断路器设备 CircuitBreak

```typescript
interface CircuitBreak extends Device {
  deviceType: DeviceType.CircuitBreak;
  /** 断路器地址 */
  address: number;
  /** RS485网关ID（必填） */
  rs485GatewayId: number;
}
```

**注意：** CircuitBreak 没有 selfId 字段

**创建设备 DTO (CreateCircuitBreak)：**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| deviceName | string | 是 | 设备名称 |
| deviceType | DeviceType | 是 | 固定为 CircuitBreak |
| belongToLaboratoryId | number | 是 | 所属实验室ID |
| groupId | string | 否 | 机组ID |
| address | number | 是 | 断路器地址（11-30） |
| rs485GatewayId | number | 是 | RS485网关ID |

### 9. 创建设备多态 DTO (CreateDevice)

使用 Jackson 多态反序列化，前端提交时必须包含 `deviceType` 字段：

```typescript
// 创建设备请求体联合类型
type CreateDeviceRequest = 
  | (CreateAirCondition & { deviceType: 'AirCondition' })
  | (CreateLight & { deviceType: 'Light' })
  | (CreateAccess & { deviceType: 'Access' })
  | (CreateSensor & { deviceType: 'Sensor' })
  | (CreateCircuitBreak & { deviceType: 'CircuitBreak' });

// 示例：创建空调
const createAirConditionRequest = {
  deviceType: 'AirCondition',
  deviceName: '中央空调A1',
  belongToLaboratoryId: 200,
  address: 35,
  selfId: 1,
  rs485GatewayId: 300,
  groupId: 'group-01'
};

// 示例：创建照明
const createLightRequest = {
  deviceType: 'Light',
  deviceName: '照明灯L1',
  belongToLaboratoryId: 200,
  address: 41,
  selfId: 1,
  rs485GatewayId: 300
};
```

### 10. 编辑设备 DTO (UpdateDevice)

```typescript
interface UpdateDevice {
  /** 设备ID（必填） */
  deviceId: number;
  /** 设备名称（可选） */
  deviceName?: string;
  /** 是否启用轮询检测（可选） */
  pollingEnabled?: boolean;
}
```

**业务说明：**
- 当 `pollingEnabled` 从 `true` 改为 `false` 时，会自动取消设备的轮询任务
- 当 `pollingEnabled` 从 `false` 改为 `true` 时，会自动启动设备的轮询任务
- 仅修改 `deviceName` 时不会影响轮询状态

### 11. 删除设备 DTO (DeleteDevice)

```typescript
interface DeleteDevice {
  /** 设备ID（必填） */
  deviceId: number;
}
```

**业务说明：** 删除设备时会自动取消该设备的轮询任务，并级联删除设备记录。

### 12. 设备记录 DeviceRecord（实时状态）

设备记录展示设备的实时运行状态，查询设备列表时返回。

#### 12.1 空调记录 AirConditionRecord

```typescript
interface AirConditionRecord {
  id: number;
  deviceId: number;
  address: number;
  selfId: number;
  /** 是否开启: true/false */
  isOpen: boolean;
  /** 模式: 1=制冷,2=制热,3=除湿,4=送风,5=自动 */
  mode: string;
  /** 设定温度: 16-30℃ */
  temperature: number;
  /** 风速: 1=低,2=中,3=高 */
  speed: string;
  /** 房间温度: 16-30℃ */
  roomTemperature: number;
  /** 错误码: 0=正常,1=设备故障,2=通信失败 */
  errorCode: number;
  createTime: string;
}
```

#### 12.2 照明记录 LightRecord

```typescript
interface LightRecord {
  id: number;
  deviceId: number;
  address: number;
  selfId: number;
  /** 是否开启 */
  isOpen: boolean;
  /** 是否锁定 */
  isLock: boolean;
  createTime: string;
}
```

#### 12.3 门禁记录 AccessRecord

```typescript
interface AccessRecord {
  id: number;
  deviceId: number;
  address: number;
  /** 门禁状态: true=开,false=关 */
  isOpen: boolean;
  /** 门禁通电状态: true=通电,false=断电 */
  isLock: boolean;
  /** 门禁锁定状态 */
  lockStatus: number;
  /** 延时时间(秒) */
  delayTime: number;
  createTime: string;
}
```

#### 12.4 断路器记录 CircuitBreakRecord

```typescript
interface CircuitBreakRecord {
  id: number;
  deviceId: number;
  address: number;
  /** 设备断开状态 */
  isOpen: boolean;
  /** 设备是否维修 */
  isFix: boolean;
  /** 设备是否锁定 */
  isLock: boolean;
  /** 电压(V) */
  voltage: number;
  /** 电流(A) */
  current: number;
  /** 功率(W) */
  power: number;
  /** 能耗量(kWh) */
  energy: number;
  /** 漏电流(mA) */
  leakage: number;
  /** 线温(℃) */
  temperature: number;
  createTime: string;
}
```

#### 12.5 传感器记录 SensorRecord

```typescript
interface SensorRecord {
  id: number;
  deviceId: number;
  address: number;
  selfId: number;
  /** 温度(℃) */
  temperature: number;
  /** 湿度(%) */
  humidity: number;
  /** 光照强度(lux) */
  light: number;
  /** 烟雾(0-100) */
  smoke: number;
  createTime: string;
}
```

### 13. VO 对象

#### 13.1 设备视图 DeviceVo

查询设备列表返回的视图对象：

```typescript
interface DeviceVo {
  /** 设备完整信息（根据类型可能是 AirCondition/Light/Access/Sensor/CircuitBreak） */
  device: Device | AirCondition | Light | Access | Sensor | CircuitBreak;
  /** 设备实时记录（根据设备类型不同） */
  deviceRecord: DeviceRecordVo<any>;
}

interface DeviceRecordVo<T> {
  data: T;
  deviceType: DeviceType;
}
```

**DeviceVo 示例：**
```typescript
{
  device: {
    id: 500,
    deviceName: "中央空调A1",
    deviceType: "AirCondition",
    belongToLaboratoryId: 200,
    pollingEnabled: true,
    address: 35,
    selfId: 1,
    rs485GatewayId: 300,
    socketGatewayId: null,
    groupId: "group-01",
    isLock: false,
    createTime: "2025-01-15T10:30:00",
    updateTime: "2025-01-15T10:30:00"
  },
  deviceRecord: {
    data: {
      isOpen: true,
      mode: "制冷",
      temperature: 26,
      speed: "低",
      roomTemperature: 25,
      errorCode: 0,
      address: 35,
      selfId: 1
    },
    deviceType: "AirCondition"
  }
}
```

#### 13.2 RS485网关视图 Rs485GatewayVo

```typescript
interface Rs485GatewayVo {
  /** 网关ID */
  gatewayId: number;
  /** 所属实验室ID */
  laboratoryId: number;
  /** 网关名称 */
  gatewayName: string;
  /** 接收主题（MQTT订阅） */
  acceptTopic: string;
  /** 发送主题（MQTT发布） */
  sendTopic: string;
}
```

#### 13.3 Socket网关视图 SocketGatewayVo

```typescript
interface SocketGatewayVo {
  /** 网关ID */
  gatewayId: number;
  /** 网关名称 */
  gatewayName: string;
  /** 所属实验室ID */
  laboratoryId: number;
  /** MAC地址 */
  mac: string;
  /** IP地址 */
  ip: string;
}
```

---

## 网关管理数据模型

### 网关类型定义

```typescript
// 网关类型枚举
enum GatewayType {
  RS485 = 'RS485',
  Socket = 'Socket'
}

// 网关类型配置
const GatewayTypeConfig: Record<GatewayType, { label: string; description: string }> = {
  [GatewayType.RS485]: { 
    label: 'RS485网关', 
    description: '通过MQTT协议通信，支持多个设备接入' 
  },
  [GatewayType.Socket]: { 
    label: 'Socket网关', 
    description: '通过TCP Socket通信，主要用于空调设备' 
  }
};
```

### RS485 网关实体

```typescript
interface RS485Gateway {
  id: number;
  gatewayName: string;
  sendTopic: string;        // MQTT发送主题
  acceptTopic: string;      // MQTT接收主题
  belongToLaboratoryId: number;
  createTime?: string;
  updateTime?: string;
}
```

**创建 RS485 网关 DTO (CreateRS485Gateway)：**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| gatewayName | string | 是 | 网关名称 |
| sendTopic | string | 是 | MQTT发送主题（设备接收指令） |
| acceptTopic | string | 是 | MQTT接收主题（设备上报数据） |
| belongToLaboratoryId | number | 是 | 所属实验室ID |

**主题命名规范示例：**
- 发送主题: `lab/rs485/send1`
- 接收主题: `lab/rs485/accept1`

### Socket 网关实体

> ⚠️ **注意：Socket 网关功能尚未实现，以下接口定义仅供参考**

```typescript
interface SocketGateway {
  id: number;
  gatewayName: string;
  mac: string;              // MAC地址
  ip: string;               // IP地址（动态分配）
  belongToLaboratoryId: number;
  createTime?: string;
  updateTime?: string;
}
```

**创建 Socket 网关 DTO (CreateSocketGateway)：**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| gatewayName | string | 是 | 网关名称 |
| mac | string | 是 | MAC地址（如 00-11-22-33-44-55） |
| belongToLaboratoryId | number | 是 | 所属实验室ID |

**说明：**
- Socket 网关的 IP 地址由设备连接时动态上报
- MAC 地址作为设备的唯一标识

### 删除网关 DTO

```typescript
// 删除 RS485 网关
interface DeleteRS485GatewayDTO {
  rs485GatewayId: number;
}

// 删除 Socket 网关
interface DeleteSocketGatewayDTO {
  socketGatewayId: number;
}
```

---

## API 接口

### 网关管理接口

#### 1. POST /gateway/create/rs485 - 创建 RS485 网关

**权限**: `DEVICE_ADD`

**请求体**: `CreateRS485Gateway`

```typescript
const createRS485Data: CreateRS485Gateway = {
  gatewayName: 'RS485-1',
  sendTopic: 'lab/rs485/send1',
  acceptTopic: 'lab/rs485/accept1',
  belongToLaboratoryId: 200
};
```

**响应**: `DiyResponseEntity<RS485Gateway>`

```typescript
{
  body: {
    code: 200,
    message: 'success',
    data: {
      id: 300,
      gatewayName: 'RS485-1',
      sendTopic: 'lab/rs485/send1',
      acceptTopic: 'lab/rs485/accept1',
      belongToLaboratoryId: 200,
      createTime: '2025-01-15T10:30:00'
    },
    success: true
  },
  statusCode: 200
}
```

#### 2. POST /gateway/create/socket - 创建 Socket 网关

> ⚠️ **注意：此接口为预留接口，后端功能尚未实现**

**权限**: `DEVICE_ADD`

**请求体**: `CreateSocketGateway`

```typescript
const createSocketData: CreateSocketGateway = {
  gatewayName: 'SocketGW-1',
  mac: '00-11-22-33-44-55',
  belongToLaboratoryId: 200
};
```

**响应**: `DiyResponseEntity<SocketGateway>`

```typescript
{
  body: {
    code: 200,
    message: 'success',
    data: {
      id: 400,
      gatewayName: 'SocketGW-1',
      mac: '00-11-22-33-44-55',
      ip: '10.0.0.10',
      belongToLaboratoryId: 200,
      createTime: '2025-01-15T10:30:00'
    },
    success: true
  },
  statusCode: 200
}
```

#### 3. DELETE /gateway/delete/rs485 - 删除 RS485 网关

**权限**: `DEVICE_ADD`

**请求体**: `DeleteRS485GatewayDTO`

```typescript
const deleteRS485Data: DeleteRS485GatewayDTO = {
  rs485GatewayId: 300
};
```

**响应**: `DiyResponseEntity<void>`

**业务说明：**
- 删除网关时会级联删除关联的设备
- 确保没有设备正在使用该网关

#### 4. DELETE /gateway/delete/socket - 删除 Socket 网关

> ⚠️ **注意：此接口为预留接口，后端功能尚未实现**

**权限**: `DEVICE_ADD`

**请求体**: `DeleteSocketGatewayDTO`

```typescript
const deleteSocketData: DeleteSocketGatewayDTO = {
  socketGatewayId: 400
};
```

**响应**: `DiyResponseEntity<void>`

---

### 设备管理接口

#### 1. 创建设备

**接口：** `POST /device/create`

**权限：** `DEVICE_ADD`

**请求体：** CreateDevice（多态，根据 deviceType 字段自动反序列化为具体类型）

**创建空调示例：**
```json
{
  "deviceType": "AirCondition",
  "deviceName": "中央空调A1",
  "belongToLaboratoryId": 200,
  "address": 35,
  "selfId": 1,
  "rs485GatewayId": 300,
  "socketGatewayId": null,
  "groupId": "group-01"
}
```

**创建照明示例：**
```json
{
  "deviceType": "Light",
  "deviceName": "照明灯L1",
  "belongToLaboratoryId": 200,
  "address": 41,
  "selfId": 1,
  "rs485GatewayId": 300
}
```

**响应：** `DiyResponseEntity<R<void>>`

```typescript
{
  "body": {
    "code": 200,
    "message": "success",
    "data": null,
    "success": true
  },
  "statusCode": 200
}
```

**业务说明：**
- 创建设备时会自动初始化一条默认记录
- 如果 `pollingEnabled` 为 `true`（默认），会自动启动轮询任务
- 同一网关下 address + selfId 组合必须唯一

---

### 2. 查询设备列表

**接口：** `POST /device/list/device`

**请求参数（Query String）：**
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| laboratoryIds | number[] | 是 | 实验室ID列表（多选） |
| deviceType | DeviceType | 是 | 设备类型筛选 |

**请求示例：**
```
POST /device/list/device?laboratoryIds=200&laboratoryIds=201&deviceType=AirCondition
```

**响应：** `DiyResponseEntity<R<Map<number, DeviceVo[]>>>`

```typescript
{
  "body": {
    "code": 200,
    "message": "success",
    "data": {
      "200": [  // 实验室ID为200的设备列表
        {
          "device": {
            "id": 500,
            "deviceName": "中央空调A1",
            "deviceType": "AirCondition",
            "belongToLaboratoryId": 200,
            "pollingEnabled": true,
            "address": 35,
            "selfId": 1,
            "rs485GatewayId": 300,
            "socketGatewayId": null,
            "groupId": "group-01",
            "isLock": false
          },
          "deviceRecord": {
            "data": {
              "isOpen": true,
              "mode": "制冷",
              "temperature": 26,
              "speed": "低",
              "roomTemperature": 25,
              "errorCode": 0
            },
            "deviceType": "AirCondition"
          }
        }
      ],
      "201": [  // 实验室ID为201的设备列表
        // ...
      ]
    },
    "success": true
  },
  "statusCode": 200
}
```

**业务说明：**
- 返回数据以实验室ID为键的Map结构
- 只返回有权限查看的实验室的设备
- 包含设备的完整信息和实时状态记录

---

### 3. 删除设备

**接口：** `DELETE /device/delete`

**权限：** `DEVICE_ADD`

**请求体：**
```json
{
  "deviceId": 500
}
```

**响应：** `DiyResponseEntity<R<void>>`

```typescript
{
  "body": {
    "code": 200,
    "message": "success",
    "data": null,
    "success": true
  },
  "statusCode": 200
}
```

**业务说明：**
- 删除设备时会自动取消该设备的轮询任务
- 数据库级联删除关联的设备记录

---

### 4. 编辑设备

**接口：** `PUT /device/update`

**权限：** `DEVICE_ADD`

**请求体：**
```json
{
  "deviceId": 500,
  "deviceName": "新名称"
}
```

或修改轮询状态：
```json
{
  "deviceId": 500,
  "deviceName": "新名称",
  "pollingEnabled": false
}
```

**响应：** `DiyResponseEntity<R<Device>>`

```typescript
{
  "body": {
    "code": 200,
    "message": "success",
    "data": {
      "id": 500,
      "deviceName": "新名称",
      "deviceType": "AirCondition",
      "belongToLaboratoryId": 200,
      "pollingEnabled": false,
      // ... 其他字段
    },
    "success": true
  },
  "statusCode": 200
}
```

**业务说明：**
- 修改 `pollingEnabled` 会联动控制轮询任务的启动/取消
- 从 `true` 改为 `false`：停止轮询
- 从 `false` 改为 `true`：启动轮询
- 返回更新后的设备完整信息

---

### 5. 查询 RS485 网关列表

**接口：** `GET /device/list/rs485`

**权限：** `DEVICE_ADD`

**响应：** `DiyResponseEntity<R<Map<number, Rs485GatewayVo[]>>>`

```typescript
{
  "body": {
    "code": 200,
    "message": "success",
    "data": {
      "200": [  // 实验室ID为200的网关列表
        {
          "gatewayId": 300,
          "laboratoryId": 200,
          "gatewayName": "RS485-1",
          "acceptTopic": "lab/rs485/accept1",
          "sendTopic": "lab/rs485/send1"
        },
        {
          "gatewayId": 301,
          "laboratoryId": 200,
          "gatewayName": "RS485-2",
          "acceptTopic": "lab/rs485/accept2",
          "sendTopic": "lab/rs485/send2"
        }
      ]
    },
    "success": true
  },
  "statusCode": 200
}
```

**业务说明：**
- 只返回当前用户可见实验室的网关
- 用于创建设备时的网关选择

---

### 6. 查询 Socket 网关列表

> ⚠️ **注意：此接口为预留接口，后端功能尚未实现**

**接口：** `GET /device/list/socket`

**权限：** `DEVICE_ADD`

**响应：** `DiyResponseEntity<R<Map<number, SocketGatewayVo[]>>>`

```typescript
{
  "body": {
    "code": 200,
    "message": "success",
    "data": {
      "200": [
        {
          "gatewayId": 400,
          "gatewayName": "SocketGW-1",
          "laboratoryId": 200,
          "mac": "00-11-22-33-44-55",
          "ip": "10.0.0.10"
        }
      ]
    },
    "success": true
  },
  "statusCode": 200
}
```

**业务说明：**
- 仅空调设备可选择Socket网关
- 其他设备类型仅支持RS485网关

---

### 7. 开启设备轮询

**接口：** `POST /device/polling/start`

**请求参数（Query String）：**
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| deviceId | number | 是 | 设备ID |

**请求示例：**
```
POST /device/polling/start?deviceId=500
```

**响应：** `DiyResponseEntity<R<void>>`

**业务说明：**
- 设置设备的 `pollingEnabled` 为 `true`
- 启动该设备的轮询任务
- 如果已经是启用状态，则不进行任何操作

---

### 8. 关闭设备轮询

**接口：** `POST /device/polling/stop`

**请求参数（Query String）：**
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| deviceId | number | 是 | 设备ID |

**请求示例：**
```
POST /device/polling/stop?deviceId=500
```

**响应：** `DiyResponseEntity<R<void>>`

**业务说明：**
- 设置设备的 `pollingEnabled` 为 `false`
- 取消该设备的轮询任务
- 确保调度器中没有残留任务

---

## 轮询机制说明

### 轮询原理

1. **默认状态：** 新创建的设备 `pollingEnabled` 默认为 `true`，会自动启动轮询
2. **轮询任务：** 系统会定期发送数据请求指令获取设备最新状态
3. **轮询间隔：** 由后端 `PollingProperties` 配置控制（通常30秒-5分钟）

### 各设备类型的轮询指令

| 设备类型 | 轮询指令 | 说明 |
|---------|---------|------|
| AirCondition | REQUEST_AIR_CONDITION_DATA_RS485 / REQUEST_AIR_CONDITION_DATA_SOCKET | 获取空调运行状态 |
| Light | REQUEST_LIGHT_DATA | 获取照明开关状态 |
| Access | REQUEST_ACCESS_DATA | 获取门禁状态 |
| Sensor | REQUEST_SENSOR_DATA | 获取环境数据 |
| CircuitBreak | REQUEST_CIRCUITBREAK_DATA | 获取电气参数 |

### 控制轮询的方式

1. **通过编辑设备：** 修改 `pollingEnabled` 字段（/device/update）
2. **通过独立接口：** /device/polling/start 和 /device/polling/stop

**使用场景：**
- 临时禁用故障设备的轮询，避免日志刷屏
- 批量管理设备的在线检测状态

---

## 前端组件设计建议

### 1. 设备列表组件

**功能设计：**
```
┌─────────────────────────────────────────────────────────────┐
│ 设备管理                                    [+ 新增设备]    │
├─────────────────────────────────────────────────────────────┤
│ 实验室: [全部 ▼] [实验室A ▼] [实验室B ▼]                     │
│ 类型:   [全部 ▼] 状态: [全部 ▼]                             │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐            │
│ │  中央空调A1  │ │  照明灯L1    │ │  门禁X1      │            │
│ │  [运行中]    │ │  [已关闭]    │ │  [在线]      │            │
│ │  室温: 25℃  │ │              │ │              │            │
│ │  设定: 26℃  │ │  轮询: ✓     │ │  轮询: ✓     │            │
│ │             │ │              │ │              │            │
│ │ [编辑][删除]│ │ [编辑][删除] │ │ [编辑][删除] │            │
│ └─────────────┘ └─────────────┘ └─────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

**表格列建议：**
| 列名 | 说明 | 排序 |
|-----|------|------|
| 设备名称 | 显示设备名称 | ✓ |
| 类型 | 设备类型标签 | ✓ |
| 实验室 | 所属实验室 | ✓ |
| 地址/编号 | address + selfId | - |
| 网关 | 关联的RS485/Socket网关 | - |
| 状态 | 在线/离线/故障 | ✓ |
| 轮询 | Switch开关控制 | - |
| 操作 | 编辑/删除 | - |

### 2. 创建设备向导

**步骤设计：**

```
步骤1: 选择设备类型
┌─────────────────────────────────────────────┐
│                                             │
│   [空调图标]  [照明图标]  [门禁图标]          │
│   空调        照明        门禁              │
│                                             │
│   [传感器图标] [断路器图标]                   │
│   传感器      断路器                        │
│                                             │
└─────────────────────────────────────────────┘

步骤2: 基础信息
┌─────────────────────────────────────────────┐
│  设备名称: [________________]               │
│  所属实验室: [请选择实验室 ▼]               │
│  机组ID: [________________] (选填)          │
└─────────────────────────────────────────────┘

步骤3: 通信配置（根据类型动态变化）
┌─────────────────────────────────────────────┐
│  空调配置:                                   │
│  地址: [___] (31-40)                        │
│  编号: [___]                                │
│  RS485网关: [请选择 ▼] (可选)               │
│  Socket网关: [请选择 ▼] (可选) ⚠️ 未实现    │
│  ⚠️ 两个网关至少填写一个                     │
└─────────────────────────────────────────────┘

步骤4: 确认创建
┌─────────────────────────────────────────────┐
│  设备名称: 中央空调A1                        │
│  类型: 空调                                  │
│  实验室: 空调控制实验室                      │
│  地址/编号: 35/1                             │
│  RS485网关: RS485-1                          │
│                                             │
│  [上一步] [确认创建]                         │
└─────────────────────────────────────────────┘
```

### 3. 编辑设备弹窗

```
┌─────────────────────────────────────────────┐
│ 编辑设备 - 中央空调A1            [X]        │
├─────────────────────────────────────────────┤
│  设备ID: 500 (只读)                          │
│  设备类型: 空调 (只读)                       │
│                                             │
│  设备名称: [中央空调A1_______]               │
│                                             │
│  所属实验室: 空调控制实验室 (不可修改)        │
│                                             │
│  轮询检测: [开/关]                           │
│  说明: 关闭后设备状态将不再自动更新           │
│                                             │
│  [取消] [保存]                               │
└─────────────────────────────────────────────┘
```

### 4. 网关选择器组件

**RS485网关级联选择器：**
```
实验室: [请选择实验室 ▼]
       ↓ 选择后
RS485网关: [请选择网关 ▼]
          ├─ RS485-1 (lab/rs485/accept1)
          └─ RS485-2 (lab/rs485/accept2)
```

**Socket网关级联选择器：**
```
实验室: [请选择实验室 ▼]
       ↓ 选择后
Socket网关: [请选择网关 ▼]
           └─ SocketGW-1 (00-11-22-33-44-55)
```

### 5. 设备详情抽屉/弹窗

```
┌─────────────────────────────────────────────┐
│ 设备详情 - 中央空调A1            [X]        │
├─────────────────────────────────────────────┤
│ 基础信息                                    │
│ ───────────────────────────────────────────│
│ 设备ID: 500                                │
│ 设备名称: 中央空调A1                        │
│ 设备类型: 空调                              │
│ 所属实验室: 空调控制实验室                   │
│ 创建时间: 2025-01-15 10:30:00              │
│                                             │
│ 通信配置                                    │
│ ───────────────────────────────────────────│
│ 地址: 35                                   │
│ 编号: 1                                    │
│ RS485网关: RS485-1                         │
│ Socket网关: -                              │
│ 机组ID: group-01                           │
│                                             │
│ 当前状态                                    │
│ ───────────────────────────────────────────│
│ 开关: 开启                                 │
│ 模式: 制冷                                 │
│ 设定温度: 26℃                              │
│ 室温: 25℃                                  │
│ 风速: 低风                                  │
│ 故障码: 正常 (0)                           │
│ 更新时间: 2025-01-15 10:35:22              │
└─────────────────────────────────────────────┘
```

### 6. 网关管理组件

#### 网关列表组件

```
┌─────────────────────────────────────────────────────────────┐
│ 网关管理                                                    │
├─────────────────────────────────────────────────────────────┤
│ 实验室: [全部 ▼] 类型: [全部 ▼]                            │
├─────────────────────────────────────────────────────────────┤
│ RS485网关                                                   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ RS485-1                                    [编辑][删除] │ │
│ │ 实验室: 空调控制实验室                                  │ │
│ │ 发送主题: lab/rs485/send1                               │ │
│ │ 接收主题: lab/rs485/accept1                             │ │
│ │ 关联设备: 5台                                           │ │
│ └─────────────────────────────────────────────────────────┘ │
│ Socket网关                                                  │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ SocketGW-1                                 [编辑][删除] │ │
│ │ 实验室: 空调控制实验室                                  │ │
│ │ MAC: 00-11-22-33-44-55                                  │ │
│ │ IP: 10.0.0.10                                           │ │
│ │ 关联设备: 2台                                           │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### 创建 RS485 网关弹窗

```
┌─────────────────────────────────────────────┐
│ 创建 RS485 网关                  [X]        │
├─────────────────────────────────────────────┤
│                                             │
│  网关名称: [RS485-1________]               │
│                                             │
│  所属实验室: [请选择实验室 ▼]              │
│                                             │
│  发送主题: [lab/rs485/send1___]            │
│  说明: 用于向设备发送控制指令               │
│                                             │
│  接收主题: [lab/rs485/accept1_]            │
│  说明: 用于接收设备上报数据                 │
│                                             │
│  [取消] [确认创建]                          │
└─────────────────────────────────────────────┘
```

#### 创建 Socket 网关弹窗

```
┌─────────────────────────────────────────────┐
│ 创建 Socket 网关                 [X]        │
├─────────────────────────────────────────────┤
│                                             │
│  网关名称: [SocketGW-1_______]             │
│                                             │
│  所属实验室: [请选择实验室 ▼]              │
│                                             │
│  MAC地址: [00-11-22-33-44-55]              │
│  说明: 设备的唯一物理地址                   │
│                                             │
│  [取消] [确认创建]                          │
└─────────────────────────────────────────────┘
```

---

## 网关管理 TypeScript 定义补充

### 网关管理 API 类型

```typescript
// 网关 API 路径
const GatewayAPI = {
  CREATE_RS485: '/gateway/create/rs485',
  CREATE_SOCKET: '/gateway/create/socket',
  DELETE_RS485: '/gateway/delete/rs485',
  DELETE_SOCKET: '/gateway/delete/socket'
} as const;

// 网关 ApiClient 扩展
interface GatewayApiClient {
  // 创建 RS485 网关
  createRS485Gateway(data: CreateRS485Gateway): Promise<DiyResponseEntity<RS485Gateway>>;
  
  // 创建 Socket 网关
  createSocketGateway(data: CreateSocketGateway): Promise<DiyResponseEntity<SocketGateway>>;
  
  // 删除 RS485 网关
  deleteRS485Gateway(data: DeleteRS485GatewayDTO): Promise<DiyResponseEntity<void>>;
  
  // 删除 Socket 网关
  deleteSocketGateway(data: DeleteSocketGatewayDTO): Promise<DiyResponseEntity<void>>;
}
```

### 网关表单验证规则

```typescript
// RS485 网关创建验证
const createRS485Rules: FormRules = {
  gatewayName: [
    { required: true, message: '请输入网关名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  sendTopic: [
    { required: true, message: '请输入发送主题', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9/_-]+$/, message: '主题只能包含字母、数字、下划线和斜杠', trigger: 'blur' }
  ],
  acceptTopic: [
    { required: true, message: '请输入接收主题', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9/_-]+$/, message: '主题只能包含字母、数字、下划线和斜杠', trigger: 'blur' }
  ],
  belongToLaboratoryId: [
    { required: true, message: '请选择所属实验室', trigger: 'change' }
  ]
};

// Socket 网关创建验证
const createSocketRules: FormRules = {
  gatewayName: [
    { required: true, message: '请输入网关名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  mac: [
    { required: true, message: '请输入MAC地址', trigger: 'blur' },
    { pattern: /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/, message: 'MAC地址格式不正确', trigger: 'blur' }
  ],
  belongToLaboratoryId: [
    { required: true, message: '请选择所属实验室', trigger: 'change' }
  ]
};
```

---

## 权限说明

| 权限编码 | 说明 | 相关接口 |
|---------|------|---------|
| DEVICE_ADD | 设备增删改 | /device/create, /device/update, /device/delete, /device/list/rs485, /device/list/socket |

**说明：** 设备查询和轮询控制接口不需要特殊权限，但查询结果会根据用户的实验室权限进行过滤。

---

## 数据验证规则

### 创建设备验证

1. **基础字段**
   - deviceName: 不能为空
   - deviceType: 不能为空，必须是有效枚举值
   - belongToLaboratoryId: 不能为空，用户必须有该实验室权限

2. **地址字段**
   - address: 不能为空，必须在设备类型的地址范围内
   - selfId: 除 CircuitBreak 外必填

3. **网关字段**
   - AirCondition: rs485GatewayId 和 socketGatewayId 至少填一个
   - 其他设备: rs485GatewayId 必填

4. **唯一性检查**
   - 同一RS485网关下，address + selfId 组合必须唯一
   - 同一Socket网关下，address + selfId 组合必须唯一

### 编辑设备验证

- deviceId: 不能为空，设备必须存在
- deviceName: 可选，不为空时更新
- pollingEnabled: 可选，布尔值

---

## 注意事项

1. **设备类型与网关的对应关系：**
   - 空调：支持 RS485 和 Socket 两种网关（至少填一种）⚠️ **Socket 网关尚未实现**
   - 照明/门禁/传感器/断路器：仅支持 RS485 网关

2. **地址范围：**
   - AirCondition: 31-40 (0x1F-0x28)
   - CircuitBreak: 11-30 (0x0B-0x1E)
   - Light: 41-60 (0x29-0x3C)
   - Sensor: 61-80 (0x3D-0x50)
   - Access: 1-10 (0x01-0x0A)

3. **默认记录初始化：**
   - 创建设备时会自动初始化一条默认记录到设备记录表
   - 空调默认：关闭、自动模式、26℃、低风、室温26℃、无故障
   - 照明默认：关闭、未锁定
   - 其他设备：根据类型有相应的默认值

4. **轮询任务生命周期：**
   - 创建时：如果 pollingEnabled=true（默认），自动启动轮询
   - 编辑时：修改 pollingEnabled 会联动启动/停止轮询
   - 删除时：自动取消轮询任务并清理资源

5. **越权检查：**
   - 查询设备列表时，只返回用户可见实验室的设备
   - 查询网关列表时，只返回可见实验室的网关
   - 尝试操作无权限实验室的设备会返回"查询越权"错误

6. **多态序列化：**
   - 创建设备时必须包含 `deviceType` 字段
   - 后端使用 Jackson 多态反序列化自动映射到具体类型
   - 字段必须严格符合各设备类型的 DTO 定义

## 网关管理补充说明

### 网关与设备的关系

```
┌─────────────────────────────────────────────────────────────┐
│                     网关与设备关系图                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   RS485网关 (RS485Gateway)                                   │
│   ├─ 可以连接多个设备                                         │
│   ├─ 设备类型: AirCondition, Light, Access, Sensor, CircuitBreak │
│   └─ 通信方式: MQTT (sendTopic / acceptTopic)                │
│                                                             │
│   Socket网关 (SocketGateway)   ⚠️ 尚未实现                   │
│   ├─ 主要用于空调设备 (AirCondition)                          │
│   ├─ 通信方式: TCP Socket                                     │
│   └─ 通过MAC地址识别设备                                      │
│                                                             │
│   空调设备特殊说明:                                           │
│   ├─ 可以同时关联 RS485网关 和 Socket网关                     │
│   ├─ 两个网关至少填写一个                                     │
│   └─ 实现双通道通信冗余                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 网关删除限制

1. **级联删除风险：**
   - 删除网关会级联删除该网关下的所有设备
   - 删除前需要确认没有重要设备在使用该网关

2. **删除前检查：**
   - 建议前端在删除网关时显示关联设备列表
   - 需要用户确认后才执行删除操作

### MQTT 主题规范

**推荐主题命名格式：**
```
lab/{gatewayType}/{action}/{location}/{index}

示例:
- lab/rs485/send1        (RS485网关1的发送主题)
- lab/rs485/accept1      (RS485网关1的接收主题)
- lab/rs485/send2        (RS485网关2的发送主题)
- lab/socket/acceptA     (Socket网关A的接收主题)
```

### Socket 网关 IP 动态更新

- Socket 网关创建时只需要 MAC 地址
- IP 地址由设备首次连接时自动上报
- 前端显示 Socket 网关时，如果 IP 为空表示设备尚未上线
