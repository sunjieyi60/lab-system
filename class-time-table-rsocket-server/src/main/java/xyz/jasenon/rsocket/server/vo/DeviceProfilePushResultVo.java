package xyz.jasenon.rsocket.server.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 班牌档案推送/落库结果（HTTP data 强类型，避免 Map）
 */
@Getter
@Setter
public class DeviceProfilePushResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 触发类型：CONFIG / LABORATORY */
    private String trigger;

    private String uuid;
    private boolean success;
    /** 与 PushConfigResult.code 一致：200 已应用、201 离线已落库、404 等 */
    private int outcomeCode;
    private String msg;
    private String desc;

    /**
     * 本次请求中已写入数据库的档案字段
     */
    private List<String> persistedDatabaseFields = new ArrayList<>();

    /**
     * 当时是否判定设备在线并尝试 RSocket 下行（false 表示离线，仅落库）
     */
    private boolean deviceOnline;

    /**
     * 是否已发起下行（在线时尝试 sendTo；离线为 false）
     */
    private boolean pushAttempted;

    /**
     * 通知设备时下行的档案项 key
     */
    private List<String> devicePayloadFields = new ArrayList<>();

    /**
     * 与 devicePayloadFields 一一对应的中文说明
     */
    private List<String> devicePayloadDescriptions = new ArrayList<>();

    public static String describePayloadKey(String key) {
        if (key == null) {
            return "";
        }
        return switch (key) {
            case "config" -> "班牌业务配置（密码、人脸精度、超时等）";
            case "laboratoryId" -> "关联实验室 ID（服务端登记）";
            default -> key;
        };
    }
}
