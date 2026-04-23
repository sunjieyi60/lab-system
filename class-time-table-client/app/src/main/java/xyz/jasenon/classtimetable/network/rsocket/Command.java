package xyz.jasenon.classtimetable.network.rsocket;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RSocket 命令类型枚举
 * 
 * 对应各种业务操作命令
 */
public enum Command {

    /**
     * 设备注册
     */
    REGISTER(1, "设备注册"),

    /**
     * 心跳
     */
    HEARTBEAT(2, "心跳"),

    /**
     * 开门
     */
    OPEN_DOOR(3, "开门"),

    /**
     * 更新配置
     */
    UPDATE_CONFIG(4, "更新配置"),

    /**
     * 更新人脸库
     */
    UPDATE_FACE_LIBRARY(5, "更新人脸库"),

    /**
     * 更新课表
     */
    UPDATE_SCHEDULE(6, "更新课表"),

    /**
     * 设备重启
     */
    REBOOT(7, "设备重启"),

    /**
     * 远程截图
     */
    SCREENSHOT(8, "远程截图"),

    /**
     * 设备状态上报
     */
    STATUS_REPORT(9, "设备状态上报"),

    /**
     * 通用响应
     */
    RESPONSE(100, "通用响应"),

    /**
     * 初始化上传任务
     */
    INIT_UPLOAD_TASK(10, "初始化上传任务"),

    /**
     * 上传人脸图片分片
     */
    UPLOAD_FACE_IMAGE(11, "上传人脸图片"),

    /**
     * 完成上传任务
     */
    COMPLETE_UPLOAD_TASK(12, "完成上传任务");

    private final Integer code;
    private final String desc;

    Command(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private static final Map<Integer, Command> CACHE = Arrays.stream(values())
            .collect(Collectors.toMap(Command::getCode, cmd -> cmd));

    private Integer getCode() {
        return this.code;
    }

    public static Command valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        return CACHE.getOrDefault(code, null);
    }
}
