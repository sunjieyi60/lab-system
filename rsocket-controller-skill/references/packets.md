# Packet 入参出参详细定义

所有 Packet **直接继承 `Message`**，字段即业务数据，不通过嵌套 data 对象包装。

---

## 设备注册

### RegisterRequest（Client → Server）

```java
@Getter @Setter
public class RegisterRequest extends Message implements ClientSend {
    private String uuid;           // 班牌唯一编号
    private Long laboratoryId;     // 关联的实验室id

    @Override public String route() {
        return Const.Route.DEVICE_REGISTER;
    }
}
```

### RegisterResponse（Server → Client）

```java
@Getter @Setter
public class RegisterResponse extends Message implements ServerSend<RegisterResponse> {
    private String uuid;           // 班牌唯一编号
    private Config config;         // 班牌配置
    private Long laboratoryId;     // 服务端登记的关联实验室

    public RegisterResponse() { init(this); }
    @Override public Command command() { return Command.REGISTER; }
}
```

---

## 心跳

### Heartbeat（双向共用）

```java
@Getter @Setter
public class Heartbeat extends Message implements ServerSend<Heartbeat>, ClientSend {
    private String uuid;           // 班牌唯一编号
    private Integer interval;      // 心跳间隔（秒）

    public Heartbeat() { init(this); }
    @Override public String route() { return super.getRoute(); }
    @Override public Command command() { return super.getCommand(); }
}
```

---

## 配置更新

### UpdateConfigRequest（Server → Client）

```java
@Getter @Setter
public class UpdateConfigRequest extends Message implements ServerSend {
    private Config config;         // 新配置
    private Boolean immediate;     // 是否立即生效
    private Long version;          // 配置版本号（冲突检测）
    private Long laboratoryId;     // 服务端登记的关联实验室
    private Long requestTime;      // 请求时间

    @Override public Command command() { return Command.UPDATE_CONFIG; }
}
```

### UpdateConfigResponse（Client → Server）

```java
@Getter @Setter
public class UpdateConfigResponse extends Message implements ClientSend {
    private boolean success;       // 是否成功
    private Long updateTime;       // 更新时间

    @Override public String route() { return Const.Route.DEVICE_CONFIG_UPDATE; }
}
```

---

## 开门

### OpenDoorRequest（Server → Client）

```java
@Getter @Setter
public class OpenDoorRequest extends Message implements ServerSend {
    private OpenType type;         // 开门方式：FACE/PASSWORD/REMOTE
    private String verifyInfo;     // 验证信息（人脸ID或密码）
    private Integer duration;      // 开门持续时间（秒）
    private Long requestTime;      // 请求时间

    public enum OpenType { FACE, PASSWORD, REMOTE }

    @Override public Command command() { return super.getCommand(); }
}
```

### OpenDoorResponse（Client → Server）

```java
@Getter @Setter
public class OpenDoorResponse extends Message implements ClientSend {
    private boolean success;       // 是否成功
    private Integer code;          // 结果码：0成功，其他失败
    private String messageText;    // 结果消息
    private Long openTime;         // 实际开门时间

    @Override public String route() { return Const.Route.DEVICE_DOOR_OPEN; }
}
```

---

## 人脸库更新

### UpdateFaceLibraryRequest（Server → Client）

```java
@Getter @Setter
public class UpdateFaceLibraryRequest extends Message implements ServerSend {
    private UpdateType updateType;           // FULL / INCREMENTAL
    private Long libraryVersion;             // 人脸库版本
    private List<FaceItem> faces;            // 新增/更新的人脸列表
    private List<String> deletedFaceIds;     // 需要删除的人脸ID列表
    private Long requestTime;                // 请求时间

    public enum UpdateType { FULL, INCREMENTAL }

    @Getter @Setter
    public static class FaceItem implements Serializable {
        private String faceId;
        private String name;
        private byte[] faceData;
    }

    @Override public Command command() { return Command.UPDATE_FACE_LIBRARY; }
}
```

### UpdateFaceLibraryResponse（Client → Server）

```java
@Getter @Setter
public class UpdateFaceLibraryResponse extends Message implements ClientSend {
    private boolean success;       // 是否成功
    private Long currentVersion;   // 当前版本

    @Override public String route() { return Const.Route.DEVICE_FACE_UPDATE; }
}
```

---

## 课表更新

### UpdateScheduleRequest（Server → Client）

```java
@Getter @Setter
public class UpdateScheduleRequest extends Message implements ServerSend {
    private List<CourseSchedule> schedules;  // 课表数据
    private Long effectiveTime;              // 生效时间
    private Long requestTime;                // 请求时间

    @Override public Command command() { return Command.UPDATE_SCHEDULE; }
}
```

### UpdateScheduleResponse（Client → Server）

```java
@Getter @Setter
public class UpdateScheduleResponse extends Message implements ClientSend {
    private boolean success;       // 是否成功
    private Long currentVersion;   // 当前版本

    @Override public String route() { return Const.Route.DEVICE_SCHEDULE_UPDATE; }
}
```

---

## 重启

### RebootRequest（Server → Client）

```java
@Getter @Setter
public class RebootRequest extends Message implements ServerSend {
    private Integer delaySeconds;  // 延迟重启时间（秒），0表示立即
    private String reason;         // 重启原因
    private Long requestTime;      // 请求时间

    @Override public Command command() { return Command.REBOOT; }
}
```

### RebootResponse（Client → Server）

```java
@Getter @Setter
public class RebootResponse extends Message implements ClientSend {
    private boolean success;       // 是否成功
    private Long rebootTime;       // 实际重启时间

    @Override public String route() { return Const.Route.DEVICE_REBOOT; }
}
```

---

## 文件分片上传

### UploadTaskInitRequest（Server → Client）

```java
@Getter @Setter
public class UploadTaskInitRequest extends Message implements ServerSend {
    private String taskId;         // 任务ID
    private String faceFeatureName;// 人脸特征名称
    private int totalChunks;       // 总分片数
    private long totalSize;        // 文件总大小

    @Override public Command command() { return Command.INIT_UPLOAD_TASK; }
}
```

### UploadTaskInitResponse（Client → Server）

```java
@Getter @Setter
public class UploadTaskInitResponse extends Message implements ClientSend {
    private boolean success;
    private String taskId;
}
```

### FileChunkPacket（Server → Client）

```java
@Getter @Setter
public class FileChunkPacket extends Message implements ServerSend {
    private String taskId;         // 任务ID
    private int chunkIndex;        // 分片索引
    private byte[] data;           // 分片数据

    @Override public Command command() { return Command.UPLOAD_FACE_IMAGE; }
}
```

### FileChunkResponse（Client → Server）

```java
@Getter @Setter
public class FileChunkResponse extends Message implements ClientSend {
    private boolean success;
    private int chunkIndex;
}
```

### UploadCompleteRequest（Server → Client）

```java
@Getter @Setter
public class UploadCompleteRequest extends Message implements ServerSend {
    private String taskId;         // 任务ID

    @Override public Command command() { return Command.COMPLETE_UPLOAD_TASK; }
}
```

### UploadCompleteResponse（Client → Server）

```java
@Getter @Setter
public class UploadCompleteResponse extends Message implements ClientSend {
    private boolean success;
    private int extractedFaces;    // 提取成功的人脸数量
}
```
