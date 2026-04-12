# Lab System 设备控制组件设计 Skill

## 概述

本 Skill 描述实验室管理系统中设备控制功能的前端组件设计规范，基于跨模块调用的后端架构（Service 模块 → MQTT 模块 → 硬件设备）。

## 后端架构流程

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐     ┌──────────────┐
│  前端组件        │────▶│ Service 模块      │────▶│  MQTT 模块       │────▶│  硬件设备      │
│  (Vue/React)    │     │  (端口 8088)      │     │  (端口 8089)     │     │  (RS485/MQTT)│
└─────────────────┘     └──────────────────┘     └─────────────────┘     └──────────────┘
                                │                        │
                                ▼                        ▼
                         POST /device/control      POST /task/add
                         TaskDispatch.dispatch()  MqttTaskExplainer
                         MqttTaskSendStrategy     TaskProcessorsManage
```

## 核心实体定义

### 1. Task 任务对象

```typescript
interface Task {
  /** 优先级: NORMAL(0) 用户操作, AUTOMATIC(1) 自动任务, POLLING(1) 轮询 */
  priority: 'NORMAL' | 'AUTOMATIC' | 'POLLING';
  
  /** 设备类型 */
  deviceType: 'AirCondition' | 'CircuitBreak' | 'Light' | 'Sensor' | 'Access';
  
  /** 设备ID */
  deviceId: number;
  
  /** 指令行 (CommandLine 枚举名) */
  commandLine: string;
  
  /** 参数列表 - 按 [address, selfId, ...操作参数] 顺序传递 */
  args: number[];
}
```

### 2. 设备基础类型

```typescript
// 设备基础属性
interface Device {
  id: number;
  deviceName: string;
  deviceType: DeviceType;
  belongToLaboratoryId: number;
  pollingEnabled: boolean;
}

// 各设备类型特有属性
type AirCondition = Device & {
  address: number;        // 空调地址
  selfId: number;         // 地址下编号
  rs485GatewayId: number;
  socketGatewayId?: number;
  groupId: string;        // 机组ID
  isLock: boolean;
};

type Light = Device & {
  address: number;
  selfId: number;
  rs485GatewayId: number;
  isLock: boolean;
};

type Access = Device & {
  address: number;
  rs485GatewayId: number;
  isLock: boolean;
};

type CircuitBreak = Device & {
  address: number;
  rs485GatewayId: number;
};

type Sensor = Device & {
  address: number;
  selfId: number;
  rs485GatewayId: number;
};
```

## 设备控制指令映射

### 空调控制 (AirCondition)

| 指令 | CommandLine | Args 结构 | 说明 |
|------|-------------|-----------|------|
| 打开空调 | `OPEN_AIR_CONDITION_RS485` | `[address, selfId]` | 简单开关 |
| 关闭空调 | `CLOSE_AIR_CONDITION_RS485` | `[address, selfId]` | 简单开关 |
| 增强控制 | `ENHANCE_CONTROL_AIR_CONDITION` | `[address, selfId, switch, mode, temp, speed]` | 完整控制 |
| 请求数据 | `REQUEST_AIR_CONDITION_DATA_RS485` | `[address, selfId]` | 状态查询 |

**增强控制参数详情** (`args` 下标 2-5):

```typescript
// args[2] - 开关
const SWITCH_MAP = {
  0: '关',
  1: '开'
};

// args[3] - 模式
const MODE_MAP = {
  1: '制热',
  2: '制冷', 
  4: '送风',
  8: '除湿'
};

// args[4] - 温度 (16-30)
const TEMP_RANGE = [16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30];

// args[5] - 风速
const SPEED_MAP = {
  0: '自动',
  1: '低风',
  2: '中风', 
  3: '高风'
};
```

### 门禁控制 (Access)

| 指令 | CommandLine | Args 结构 | 说明 |
|------|-------------|-----------|------|
| 单次开门 | `OPEN_ACCESS_ONCE` | `[address]` | 单次触发 |
| 单次关门 | `CLOSE_ACCESS_ONCE` | `[address]` | 单次触发 |
| 请求数据 | `REQUEST_ACCESS_DATA` | `[address]` | 状态查询 |
| 长开锁定 | `OPEN_ACCESS_PERSIST_LOCK` | `[address]` | 持续开启并锁定 |
| 长开解锁 | `OPEN_ACCESS_PERSIST_UNLOCK` | `[address]` | 持续开启可关闭 |
| 长开保持 | `OPEN_ACCESS_PERSIST_KEEP` | `[address]` | 保持当前状态 |
| 长关锁定 | `CLOSE_ACCESS_PERSIST_LOCK` | `[address]` | 持续关闭并锁定 |
| 长关解锁 | `CLOSE_ACCESS_PERSIST_UNLOCK` | `[address]` | 持续关闭可开启 |
| 长关保持 | `CLOSE_ACCESS_PERSIST_KEEP` | `[address]` | 保持当前状态 |
| 保持锁定 | `KEEP_ACCESS_STATUS_LOCK` | `[address]` | 锁定当前状态 |
| 保持解锁 | `KEEP_ACCESS_STATUS_UNLOCK` | `[address]` | 解锁当前状态 |
| 延时设定 | `SET_ACCESS_DELAY` | `[address, 0, delay]` | 设置延时时间(秒) |

### 照明控制 (Light)

| 指令 | CommandLine | Args 结构 | 说明 |
|------|-------------|-----------|------|
| 开灯 | `OPEN_LIGHT` | `[address, selfId]` | 开启指定灯 |
| 关灯 | `CLOSE_LIGHT` | `[address, selfId]` | 关闭指定灯 |
| 请求数据 | `REQUEST_LIGHT_DATA` | `[address, selfId]` | 状态查询 |

### 断路器控制 (CircuitBreak)

| 指令 | CommandLine | Args 结构 | 说明 |
|------|-------------|-----------|------|
| 合闸 | `OPEN_CIRCUITBREAK` | `[address]` | 通电 |
| 分闸 | `CLOSE_CIRCUITBREAK` | `[address]` | 断电 |
| 请求数据 | `REQUEST_CIRCUITBREAK_DATA` | `[address]` | 状态查询 |

### 传感器控制 (Sensor)

| 指令 | CommandLine | Args 结构 | 说明 |
|------|-------------|-----------|------|
| 请求数据 | `REQUEST_SENSOR_DATA` | `[address, selfId]` | 读取传感器数据 |

## 前端组件设计规范

### 1. 通用控制组件架构

```vue
<!-- DeviceControlPanel.vue -->
<template>
  <div class="device-control-panel">
    <!-- 设备类型选择器 -->
    <DeviceTypeSelector v-model="selectedType" />
    
    <!-- 动态渲染对应控制组件 -->
    <component 
      :is="controlComponentMap[selectedType]" 
      :device="selectedDevice"
      @execute="handleExecute"
    />
  </div>
</template>

<script setup>
const controlComponentMap = {
  AirCondition: markRaw(AirConditionControl),
  Access: markRaw(AccessControl),
  Light: markRaw(LightControl),
  CircuitBreak: markRaw(CircuitBreakControl),
  Sensor: markRaw(SensorControl)
};

const handleExecute = (taskPayload) => {
  // 统一发送控制指令
  return fetch('/api/device/control', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      priority: 'NORMAL',
      deviceType: selectedDevice.value.deviceType,
      deviceId: selectedDevice.value.id,
      ...taskPayload
    })
  });
};
</script>
```

### 2. 空调控制组件示例

```vue
<!-- AirConditionControl.vue -->
<template>
  <div class="ac-control">
    <!-- 快速开关 -->
    <div class="quick-actions">
      <el-button 
        type="primary" 
        :loading="loading"
        @click="executeCommand('OPEN_AIR_CONDITION_RS485')"
      >
        开机
      </el-button>
      <el-button 
        type="danger"
        :loading="loading" 
        @click="executeCommand('CLOSE_AIR_CONDITION_RS485')"
      >
        关机
      </el-button>
    </div>
    
    <!-- 增强控制面板 -->
    <div class="enhanced-control">
      <el-form :model="enhancedForm">
        <!-- 模式选择 -->
        <el-form-item label="模式">
          <el-radio-group v-model="enhancedForm.mode">
            <el-radio-button :label="1">制热</el-radio-button>
            <el-radio-button :label="2">制冷</el-radio-button>
            <el-radio-button :label="4">送风</el-radio-button>
            <el-radio-button :label="8">除湿</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <!-- 温度调节 -->
        <el-form-item label="温度">
          <el-slider 
            v-model="enhancedForm.temperature" 
            :min="16" 
            :max="30"
            show-stops
          />
        </el-form-item>
        
        <!-- 风速选择 -->
        <el-form-item label="风速">
          <el-radio-group v-model="enhancedForm.speed">
            <el-radio-button :label="0">自动</el-radio-button>
            <el-radio-button :label="1">低风</el-radio-button>
            <el-radio-button :label="2">中风</el-radio-button>
            <el-radio-button :label="3">高风</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <el-button 
          type="primary"
          :loading="loading"
          @click="executeEnhancedControl"
        >
          应用设置
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({ device: Object });
const emit = defineEmits(['execute']);

const loading = ref(false);
const enhancedForm = reactive({
  mode: 2,        // 默认制冷
  temperature: 24,
  speed: 0        // 自动
});

// 简单开关指令
const executeCommand = async (commandLine) => {
  loading.value = true;
  try {
    await emit('execute', {
      commandLine,
      args: [props.device.address, props.device.selfId]
    });
  } finally {
    loading.value = false;
  }
};

// 增强控制指令
const executeEnhancedControl = async () => {
  loading.value = true;
  try {
    await emit('execute', {
      commandLine: 'ENHANCE_CONTROL_AIR_CONDITION',
      args: [
        props.device.address,
        props.device.selfId,
        1,                          // 开机
        enhancedForm.mode,
        enhancedForm.temperature,
        enhancedForm.speed
      ]
    });
  } finally {
    loading.value = false;
  }
};
</script>
```

### 3. 门禁控制组件示例

```vue
<!-- AccessControl.vue -->
<template>
  <div class="access-control">
    <el-row :gutter="20">
      <!-- 单次控制 -->
      <el-col :span="12">
        <el-card title="单次控制">
          <el-button 
            type="success" 
            size="large"
            :loading="loading"
            @click="executeCommand('OPEN_ACCESS_ONCE', '开门成功')"
          >
            <el-icon><Open /></el-icon>
            开门
          </el-button>
          <el-button 
            type="danger" 
            size="large"
            :loading="loading"
            @click="executeCommand('CLOSE_ACCESS_ONCE', '关门成功')"
          >
            <el-icon><TurnOff /></el-icon>
            关门
          </el-button>
        </el-card>
      </el-col>
      
      <!-- 常开/常关设置 -->
      <el-col :span="12">
        <el-card title="持续状态">
          <el-form :model="persistForm">
            <el-form-item label="状态">
              <el-radio-group v-model="persistForm.state">
                <el-radio-button label="open">常开</el-radio-button>
                <el-radio-button label="close">常关</el-radio-button>
                <el-radio-button label="keep">保持</el-radio-button>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="锁定">
              <el-radio-group v-model="persistForm.lock">
                <el-radio-button label="lock">锁定</el-radio-button>
                <el-radio-button label="unlock">解锁</el-radio-button>
                <el-radio-button label="keep">保持</el-radio-button>
              </el-radio-group>
            </el-form-item>
            
            <el-button 
              type="primary"
              :loading="loading"
              @click="executePersistControl"
            >
              应用
            </el-button>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 延时设置 -->
    <el-card title="延时设置" class="mt-4">
      <el-slider 
        v-model="delayTime" 
        :max="60" 
        show-input
        :marks="{0: '0s', 30: '30s', 60: '60s'}"
      />
      <el-button 
        type="primary"
        :loading="loading"
        @click="executeDelaySet"
      >
        设置延时
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
const props = defineProps({ device: Object });
const emit = defineEmits(['execute']);

const loading = ref(false);
const persistForm = reactive({ state: 'open', lock: 'unlock' });
const delayTime = ref(5);

// CommandLine 映射
const PERSIST_COMMAND_MAP = {
  'open-lock': 'OPEN_ACCESS_PERSIST_LOCK',
  'open-unlock': 'OPEN_ACCESS_PERSIST_UNLOCK',
  'open-keep': 'OPEN_ACCESS_PERSIST_KEEP',
  'close-lock': 'CLOSE_ACCESS_PERSIST_LOCK',
  'close-unlock': 'CLOSE_ACCESS_PERSIST_UNLOCK',
  'close-keep': 'CLOSE_ACCESS_PERSIST_KEEP',
  'keep-lock': 'KEEP_ACCESS_STATUS_LOCK',
  'keep-unlock': 'KEEP_ACCESS_STATUS_UNLOCK'
};

const executeCommand = async (commandLine, successMsg) => {
  loading.value = true;
  try {
    await emit('execute', {
      commandLine,
      args: [props.device.address]
    });
    ElMessage.success(successMsg);
  } finally {
    loading.value = false;
  }
};

const executePersistControl = async () => {
  const key = `${persistForm.state}-${persistForm.lock}`;
  const commandLine = PERSIST_COMMAND_MAP[key];
  await executeCommand(commandLine, '设置成功');
};

const executeDelaySet = async () => {
  loading.value = true;
  try {
    await emit('execute', {
      commandLine: 'SET_ACCESS_DELAY',
      args: [props.device.address, 0, delayTime.value]
    });
    ElMessage.success('延时设置成功');
  } finally {
    loading.value = false;
  }
};
</script>
```

### 4. 照明控制组件示例

```vue
<!-- LightControl.vue -->
<template>
  <div class="light-control">
    <el-switch
      v-model="lightState"
      :loading="loading"
      :active-text="'开启'"
      :inactive-text="'关闭'"
      @change="handleSwitchChange"
    />
    
    <!-- 批量控制（如果设备是灯组） -->
    <el-button-group v-if="device.groupId">
      <el-button @click="batchControl('OPEN_LIGHT')">全组开启</el-button>
      <el-button @click="batchControl('CLOSE_LIGHT')">全组关闭</el-button>
    </el-button-group>
  </div>
</template>

<script setup>
const props = defineProps({ device: Object });
const emit = defineEmits(['execute']);

const loading = ref(false);
const lightState = ref(false);

const handleSwitchChange = async (val) => {
  loading.value = true;
  try {
    const commandLine = val ? 'OPEN_LIGHT' : 'CLOSE_LIGHT';
    await emit('execute', {
      commandLine,
      args: [props.device.address, props.device.selfId]
    });
  } finally {
    loading.value = false;
  }
};
</script>
```

### 5. 断路器控制组件示例

```vue
<!-- CircuitBreakControl.vue -->
<template>
  <div class="circuit-break-control">
    <el-alert
      title="断路器操作警告"
      type="warning"
      description="分闸/合闸操作将影响该区域供电，请谨慎操作"
      show-icon
      :closable="false"
      class="mb-4"
    />
    
    <div class="control-buttons">
      <el-popconfirm
        title="确认执行合闸操作？"
        confirm-button-text="确认"
        cancel-button-text="取消"
        @confirm="executeCommand('OPEN_CIRCUITBREAK')"
      >
        <template #reference>
          <el-button type="success" size="large" :loading="loading">
            <el-icon><SwitchButton /></el-icon>
            合闸 (通电)
          </el-button>
        </template>
      </el-popconfirm>
      
      <el-popconfirm
        title="确认执行分闸操作？"
        confirm-button-text="确认"
        cancel-button-text="取消"
        type="danger"
        @confirm="executeCommand('CLOSE_CIRCUITBREAK')"
      >
        <template #reference>
          <el-button type="danger" size="large" :loading="loading">
            <el-icon><CircleClose /></el-icon>
            分闸 (断电)
          </el-button>
        </template>
      </el-popconfirm>
    </div>
  </div>
</template>
```

## API 调用封装

```typescript
// api/device.ts
import type { Task, DeviceType } from '@/types/device';

export interface ControlPayload {
  commandLine: string;
  args: number[];
}

/**
 * 发送设备控制指令
 * @param deviceType 设备类型
 * @param deviceId 设备ID
 * @param payload 控制参数
 * @returns Promise
 */
export function controlDevice(
  deviceType: DeviceType,
  deviceId: number,
  payload: ControlPayload
): Promise<{ code: number; message: string }> {
  const task: Task = {
    priority: 'NORMAL',
    deviceType,
    deviceId,
    commandLine: payload.commandLine,
    args: payload.args
  };
  
  return fetch('/api/device/control', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(task)
  }).then(res => res.json());
}

/**
 * 批量控制设备
 */
export function batchControl(
  devices: Array<{ deviceType: DeviceType; deviceId: number; address: number; selfId?: number }>,
  commandLine: string
): Promise<PromiseSettledResult<any>[]> {
  const promises = devices.map(device => 
    controlDevice(device.deviceType, device.deviceId, {
      commandLine,
      args: device.selfId !== undefined 
        ? [device.address, device.selfId]
        : [device.address]
    })
  );
  return Promise.allSettled(promises);
}
```

## 注意事项

1. **权限检查**: 控制接口需要 `DEVICE_CONTROL` 权限，前端需在调用前确认用户权限

2. **参数校验**: 
   - `args` 数组不能为空
   - 不同指令对 `args` 长度要求不同（2-6个参数）
   - 空调增强控制需要 6 个参数，其他通常为 1-2 个

3. **异步处理**: 
   - 控制指令下发是异步的，返回成功仅代表任务已加入队列
   - 设备实际状态需要通过轮询或 WebSocket 获取

4. **指令兼容性**:
   - 空调支持 RS485 和 Socket 两种网关类型，但指令不同
   - 其他设备目前仅支持 RS485 网关

5. **状态锁定**:
   - 当设备 `isLock = true` 时，应禁用控制按钮并提示"设备已锁定"
   - 锁定状态通常由管理员设置，防止误操作
