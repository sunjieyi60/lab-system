package xyz.jasenon.rsocket.core;

/**
 * 常量定义
 */
public interface Const {

    interface Key {
        String SUFFIX = ":";
        String CLASS_TIME_TABLE = "class-time-table";
        String INFO = "info";
        String UUID = "uuid";
        String ID = "id";
        
        // 缓存键
        String CACHE_DEVICE_UUID_PREFIX = "class-time-table:uuid";
        String CACHE_DEVICE_ID_PREFIX = "class-time-table:id:";
    }

    interface Log {
        String TRACE_ID_KEY = "trace-id-key";
        String SUFFIX = ":";
    }

    interface Status {
        String ONLINE = "online";
        String OFFLINE = "offline";
    }

    interface Role {
        String SERVER = "server";
    }

    /**
     * RSocket 路由路径
     */
    interface Route {
        // 设备注册
        String DEVICE_REGISTER = "device.register";
        // 设备心跳
        String DEVICE_HEARTBEAT = "device.heartbeat";
        // 设备数据流
        String DEVICE_STREAM = "device.stream";
        // 双向通道
        String DEVICE_CHANNEL = "device.channel";
        // 服务器下发命令
        String DEVICE_COMMAND = "device.command";
        // 服务器推送设备配置信息更新
        String DEVICE_CONFIG_UPDATE = "device.config.update";
        // 服务器推送人脸信息更新
        String DEVICE_FACE_UPDATE = "device.face.update";
        // 服务端推送课表信息更新
        String DEVICE_SCHEDULE_UPDATE = "device.schedule.update";
        // 服务端下发开门指令
        String DEVICE_DOOR_OPEN = "device.door.open";
        // 服务端下发重启指令
        String DEVICE_REBOOT = "device.reboot";
    }

    /**
     * 默认值
     */
    interface Default {
        // 心跳间隔（秒）
        int HEARTBEAT_INTERVAL = 30;
        // 命令超时时间（毫秒）
        long COMMAND_TIMEOUT = 30000L;
        // 缓存过期时间（分钟）
        long CACHE_MINUTES = 30;
        // 空值缓存时间（分钟）
        long NULL_CACHE_MINUTES = 5;
    }

    /**
     * 错误码
     */
    interface ErrorCode {
        // 成功
        int SUCCESS = 10000;
        // 参数错误
        int PARAM_ERROR = 40001;
        // 设备不存在
        int DEVICE_NOT_FOUND = 40004;
        // 设备不在线
        int DEVICE_OFFLINE = 40005;
        // 服务器内部错误
        int INTERNAL_ERROR = 50000;
    }
}
