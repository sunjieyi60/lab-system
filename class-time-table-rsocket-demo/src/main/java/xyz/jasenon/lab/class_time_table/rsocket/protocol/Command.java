package xyz.jasenon.lab.class_time_table.rsocket.protocol;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RSocket 命令枚举
 * 定义所有班牌与管理端之间的通信命令
 */
@Getter
public enum Command {
    
    // ==================== 设备管理 ====================
    REGISTER("register", "设备注册/上线"),
    HEARTBEAT("heartbeat", "心跳上报"),
    GET_DEVICE_STATUS("getDeviceStatus", "获取设备状态"),
    
    // ==================== 课表相关 ====================
    GET_CURRENT_TIME_TABLE("getCurrentTimeTable", "获取当前课表"),
    GET_TIME_TABLE_LIST("getTimeTableList", "获取课表列表"),
    SUBSCRIBE_TIME_TABLE_UPDATES("subscribeTimeTableUpdates", "订阅课表更新"),
    
    // ==================== 人脸识别 ====================
    PUSH_FACE_DATA("pushFaceData", "下发人脸数据"),
    REMOVE_FACE("removeFace", "删除人脸"),
    GET_DEVICE_FACE_LIST("getDeviceFaceList", "获取设备人脸列表"),
    REPORT_ACCESS("reportAccess", "上报访问记录"),
    BATCH_PUSH_FACES("batchPushFaces", "批量下发人脸"),
    
    // ==================== 配置管理 ====================
    GET_DEVICE_CONFIG("getDeviceConfig", "获取设备配置"),
    UPDATE_DEVICE_CONFIG("updateDeviceConfig", "更新设备配置"),
    REBOOT_DEVICE("rebootDevice", "远程重启设备"),
    REMOTE_OPEN_DOOR("remoteOpenDoor", "远程开门"),
    
    // ==================== 双向流 ====================
    DUPLEX_CHANNEL("duplexChannel", "实时通信通道"),
    
    // ==================== 系统命令 ====================
    PING("ping", "连接测试"),
    UNKNOWN("unknown", "未知命令");
    
    private final String code;
    private final String description;
    
    Command(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 命令 code 到枚举的映射缓存
     */
    private static final Map<String, Command> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(Command::getCode, cmd -> cmd));
    
    /**
     * 根据 code 获取枚举
     */
    public static Command fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return UNKNOWN;
        }
        return CODE_MAP.getOrDefault(code, UNKNOWN);
    }
    
    /**
     * 判断是否为设备管理类命令
     */
    public boolean isDeviceManagement() {
        return this == REGISTER || this == HEARTBEAT || this == GET_DEVICE_STATUS;
    }
    
    /**
     * 判断是否为人脸相关命令
     */
    public boolean isFaceRelated() {
        return this == PUSH_FACE_DATA || this == REMOVE_FACE 
                || this == GET_DEVICE_FACE_LIST || this == REPORT_ACCESS
                || this == BATCH_PUSH_FACES;
    }
    
    /**
     * 判断是否为课表相关命令
     */
    public boolean isTimeTableRelated() {
        return this == GET_CURRENT_TIME_TABLE || this == GET_TIME_TABLE_LIST
                || this == SUBSCRIBE_TIME_TABLE_UPDATES;
    }
    
    /**
     * 判断是否为配置相关命令
     */
    public boolean isConfigRelated() {
        return this == GET_DEVICE_CONFIG || this == UPDATE_DEVICE_CONFIG
                || this == REBOOT_DEVICE || this == REMOTE_OPEN_DOOR;
    }
    
    /**
     * 判断是否为 Fire-and-Forget 模式
     */
    public boolean isFireAndForget() {
        return this == HEARTBEAT || this == REPORT_ACCESS || this == PING;
    }
    
    /**
     * 判断是否为 Stream 模式
     */
    public boolean isStream() {
        return this == SUBSCRIBE_TIME_TABLE_UPDATES || this == BATCH_PUSH_FACES;
    }
    
    /**
     * 判断是否为 Channel 模式
     */
    public boolean isChannel() {
        return this == DUPLEX_CHANNEL;
    }
}
