# 实验室管理系统服务端接口文档

## 基本信息

- Host: `http://localhost:8088`
- 统一响应体 `R<T>`：

```json
{
  "ok": true,
  "code": 0,
  "msg": "业务处理成功",
  "data": {}
}
```

- 权限校验：使用自定义注解 `@RequestPermission(allowed = {...})` 标注在控制器方法上，切面在调用前拦截，读取当前登录用户的权限（仅“子权限”，不含目录权限），校验是否包含所需权限。
- 多态入参：设备创建的入参 `CreateDevice` 为多态体：需传 `deviceType`，将路由为具体子类型 `CreateAirCondition` / `CreateLight` / `CreateAccess` / `CreateSensor` / `CreateCircuitBreak`。
- 调用流程：控制层接收请求 → 权限切面校验通过 → 参数校验（JSR 380 注解） → 分发到服务层/策略层执行 → 返回统一 `R<T>`。

---

## 用户管理

### 创建用户 POST `/user/create`（权限：`USER_ADD`）

请求：

```json
{
  "username": "system",
  "password": "123456",
  "realName": "系统",
  "email": "system@example.com",
  "phone": "13900000000",
  "createBy": 1,
  "permissions": [
    "USER_ADD","USER_EDIT","USER_DELETE",
    "SCHEDULE_CLASSES","SCHEDULE_CLASSES_VIEW","SEMESTER_SETTINGS",
    "DEVICE_ADD","DEVICE_CONTROL","DEVICE_SMART_CONTROL","DEVICE_ALARM_SETTINGS",
    "ACADEMIC_AFFAIRS_ANALYSIS","LABORATORY_POWER_CONSUMPTION","LABORATORY_CENTRAL_AIRCONDITION",
    "BASE_CUD","BASE_VIEW"
  ],
  "deptIds": [1],
  "laboratoryIds": [101]
}
```

响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

### 编辑用户 PUT `/user/edit`（权限：`USER_EDIT`）

请求：

```json
{
  "userId": 100,
  "password": "newpass123",
  "phone": "13911112222",
  "email": "sys@lab.com",
  "realName": "系统管理员",
  "permissions": ["DEVICE_ADD","DEVICE_CONTROL"],
  "deptIds": [1,2],
  "laboratoryIds": [101,102]
}
```

响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

### 删除用户 DELETE `/user/delete`（权限：`USER_DELETE`）

请求：

```json
{ "userId": 100 }
```

响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

### 登录 POST `/user/login`

请求：

```json
{ "username": "system", "password": "123456" }
```

响应：

```json
{
  "ok": true,
  "data": {
    "token": "xxx",
    "user": {
      "id": 100,
      "username": "system",
      "realName": "系统",
      "phone": "13900000000",
      "email": "system@example.com"
    }
  }
}
```

### 登出 GET `/user/logout`

响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

### 当前用户详情 GET `/user/getCurrentUserDetail`

响应：

```json
{
  "ok": true,
  "data": {
    "depts": [ { "id": 1, "deptName": "理学院" } ],
    "laboratories": [ { "id": 101, "laboratoryName": "物理实验室" } ],
    "permissions": [ { "permission": "DEVICE_ADD", "path": [19,10,11] } ]
  }
}
```

---

## 部门管理（权限：`BASE_CUD`）

### 创建部门 POST `/dept/create`

请求：

```json
{ "deptName": "理学院" }
```

响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

### 编辑部门 PUT `/dept/edit`

请求：

```json
{ "deptId": 1, "deptName": "理学院（更新）" }
```

响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

### 删除部门 DELETE `/dept/delete`

请求：

```json
{ "deptId": 1 }
```

响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

---

## 楼栋管理（权限：`BASE_CUD`）

### 创建楼栋 POST `/building/create`

请求：

```json
{ "buildingName": "A栋", "deptIds": [301,302] }
```

### 编辑楼栋 PUT `/building/edit`

请求：

```json
{ "buildingId": 10, "buildingName": "A栋-更新" }
```

统一响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

---

## 实验室管理（权限：`BASE_CUD`）

### 创建实验室 POST `/laboratory/create`

请求：

```json
{
  "laboratoryId": "10-101",
  "laboratoryName": "物理实验室",
  "belongToDeptIds": [301,302],
  "belongToBuilding": 10,
  "securityLevel": "A级",
  "classCapacity": 40,
  "area": 80
}
```

### 编辑实验室 PUT `/laboratory/edit`

请求：

```json
{
  "id": 101,
  "laboratoryId": "10-101",
  "laboratoryName": "物理实验室（更新）",
  "belongToDeptIds": [301],
  "belongToBuilding": 10,
  "securityLevel": "B级",
  "classCapacity": 42,
  "area": 82
}
```

### 删除实验室 DELETE `/laboratory/delete`

请求：

```json
{ "id": 101 }
```

统一响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

---

## 网关管理（权限：`DEVICE_ADD`）

### 创建 RS485 网关 POST `/gateway/create/rs485`

请求：

```json
{
  "gatewayName": "RS485-GW-A",
  "sendTopic": "lab/rs485/send",
  "acceptTopic": "lab/rs485/accept",
  "belongToLaboratoryId": 101
}
```

### 创建 Socket 网关 POST `/gateway/create/socket`

请求：

```json
{
  "gatewayName": "Socket-GW-A",
  "mac": "00-11-22-33-44-55",
  "belongToLaboratoryId": 101
}
```

### 删除 RS485 网关 DELETE `/gateway/delete/rs485`

请求：

```json
{ "rs485GatewayId": 5 }
```

### 删除 Socket 网关 DELETE `/gateway/delete/socket`

请求：

```json
{ "socketGatewayId": 7 }
```

统一响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

---

## 设备管理与控制

### 创建设备（多态） POST `/device/create`（权限：`DEVICE_ADD`）

公共字段：

```json
{
  "deviceName": "理化实验室空调一号",
  "deviceType": "AirCondition",
  "belongToLaboratoryId": 101
}
```

子类型示例：

- CreateAirCondition

```json
{
  "deviceName": "中央空调A1",
  "deviceType": "AirCondition",
  "belongToLaboratoryId": 101,
  "address": 35,
  "selfId": 1,
  "rs485GatewayId": 5,
  "socketGatewayId": null,
  "groupId": "group-01"
}
```

- CreateLight

```json
{
  "deviceName": "走廊灯A",
  "deviceType": "Light",
  "belongToLaboratoryId": 101,
  "address": 41,
  "selfId": 1,
  "rs485GatewayId": 5
}
```

- CreateAccess

```json
{
  "deviceName": "门禁A",
  "deviceType": "Access",
  "belongToLaboratoryId": 101,
  "address": 10,
  "selfId": 1,
  "rs485GatewayId": 5
}
```

- CreateSensor

```json
{
  "deviceName": "温湿度传感器A",
  "deviceType": "Sensor",
  "belongToLaboratoryId": 101,
  "address": 61,
  "selfId": 1,
  "rs485GatewayId": 5
}
```

- CreateCircuitBreak

```json
{
  "deviceName": "总电断路器A",
  "deviceType": "CircuitBreak",
  "belongToLaboratoryId": 101,
  "address": 12,
  "rs485GatewayId": 5
}
```

统一响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

### 删除设备 DELETE `/device/delete`（权限：`DEVICE_ADD`）

请求：

```json
{ "deviceId": 1001 }
```

响应：

```json
{ "ok": true, "msg": "业务处理成功" }
```

### 控制设备 POST `/device/control`（权限：`DEVICE_CONTROL`）

请求：

```json
{
  "priority": "NORMAL",
  "deviceType": "Light",
  "deviceId": 1001,
  "commandLine": "OPEN_LIGHT",
  "args": [41, 1]  // 先缀address selfId  没有selfId的不用填selfId
}
```

响应：

```json
{ "ok": true, "msg": "控制任务下达成功" }
```

### 网关树 GET `/device/list/rs485`（权限：`DEVICE_ADD`）

响应：

```json
{
  "ok": true,
  "data": [
    {
      "laboratory": { "id": 101, "laboratoryName": "物理实验室" },
      "gateways": [ { "id": 5, "gatewayName": "RS485-GW-A" } ]
    }
  ]
}
```

### 网关树 GET `/device/list/socket`（权限：`DEVICE_ADD`）

响应：

```json
{
  "ok": true,
  "data": [
    {
      "laboratory": { "id": 101, "laboratoryName": "物理实验室" },
      "gateways": [ { "id": 7, "gatewayName": "Socket-GW-A", "mac": "00-11-..." } ]
    }
  ]
}
```

---

## 教务管理

### 创建课程 POST `/academic/create/course`（权限：`SCHEDULE_CLASSES`）

请求：

```json
{ "courseName": "高等数学", "volume": 120, "grade": "2023级" }
```

响应：

```json
{ "ok": true, "msg": "学期创建成功" }
```

### 创建课表 POST `/academic/create/courseSchedule`（权限：`SCHEDULE_CLASSES`）

请求：

```json
{
  "semesterId": 1,
  "laboratoryId": 101,
  "weekType": "Both",
  "startWeek": 1,
  "endWeek": 16,
  "startTime": "08:00:00",
  "endTime": "09:40:00",
  "weekdays": [1,3],
  "courseId": 1001,
  "teacherId": 2001,
  "belongToDeptId": 301,
  "startSection": 1,
  "endSection": 2,
  "mark": "备注"
}
```

响应：

```json
{ "ok": true, "msg": "课程安排创建成功" }
```

冲突规则：主体重叠（同实验室或同教师）且星期交集、周次交集、单双周匹配，时间段或节次重叠时判定冲突。

### 创建学期 POST `/academic/create/semester`（权限：`SEMESTER_SETTINGS`）

请求：

```json
{ "name": "2025-2026 第1学年", "startDate": "2025-09-01", "endDate": "2026-01-15" }
```

响应：

```json
{ "ok": true, "msg": "学期创建成功" }
```

### 删除课程 DELETE `/academic/delete/course`（权限：`SCHEDULE_CLASSES`）

请求：

```json
{ "courseId": 1001 }
```

### 删除课表 DELETE `/academic/delete/courseSchedule`（权限：`SCHEDULE_CLASSES`）

请求：

```json
{ "courseScheduleId": 5001 }
```

### 删除学期 DELETE `/academic/delete/semester`（权限：`SEMESTER_SETTINGS`）

请求：

```json
{ "semesterId": 1 }
```

### 编辑课程 PUT `/academic/edit/course`（权限：`SCHEDULE_CLASSES`）

请求：

```json
{ "courseId": 1001, "courseName": "高等数学（更新）", "volume": 150, "grade": "2024级" }
```

### 编辑学期 PUT `/academic/edit/semester`（权限：`SCHEDULE_CLASSES`）

请求：

```json
{ "semesterId": 1, "name": "2025-2026 第1学年（更新）", "startDate": "2025-09-01", "endDate": "2026-01-15" }
```

### 查询课表 GET `/academic/list/courseSchedule?laboratoryIds=101,102`（权限：`SCHEDULE_CLASSES` 或 `SCHEDULE_CLASSES_VIEW`）

响应：

```json
{
  "ok": true,
  "data": [
    {
      "laboratoryId": 101,
      "courseId": 1001,
      "teacherId": 2001,
      "weekdays": [1,3],
      "startTime": "08:00:00",
      "endTime": "09:40:00",
      "startWeek": 1,
      "endWeek": 16,
      "weekType": "Both",
      "startSection": 1,
      "endSection": 2,
      "mark": "备注"
    }
  ]
}
```

### 查询实验室 GET `/academic/list/laboratories`（权限：`SCHEDULE_CLASSES` 或 `SCHEDULE_CLASSES_VIEW`）

响应：

```json
{ "ok": true, "data": [ { "id": 101, "laboratoryName": "物理实验室" } ] }
```

### 查询课程 GET `/academic/list/course`（权限：`SCHEDULE_CLASSES` 或 `SCHEDULE_CLASSES_VIEW`）

响应：

```json
{ "ok": true, "data": [ { "id": 1001, "courseName": "高等数学", "volume": 120, "grade": "2023级" } ] }
```

---

## 设备类型枚举

可选值：`AirCondition`（空调）、`CircuitBreak`（断路器）、`Light`（灯光）、`Sensor`（传感器）、`Access`（门禁）。

---

## 权限枚举与说明

请求中仅使用叶子权限（子权限），不传目录权限。常见操作权限：

- 用户：`USER_ADD` `USER_EDIT` `USER_DELETE`
- 教务：`SCHEDULE_CLASSES` `SCHEDULE_CLASSES_VIEW` `SEMESTER_SETTINGS`
- 设备：`DEVICE_ADD` `DEVICE_CONTROL` `DEVICE_SMART_CONTROL` `DEVICE_ALARM_SETTINGS`
- 基础：`BASE_CUD` `BASE_VIEW`

