package xyz.jasenon.rsocket.server.vo;

import lombok.Data;
import xyz.jasenon.lab.common.utils.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 班牌档案推送/落库内部结果，{@link #toR()} 转为 HTTP 强类型 data
 */
@Data
public class PushConfigResult {

    private static final List<String> PROFILE_DOWNLINK_KEYS = List.of("config", "laboratoryId");

    private boolean success;
    private int code;
    private String msg;
    private String desc;
    private String uuid;

    /** 本次 HTTP 操作类型，用于顶层文案与 persisted 语义 */
    private ProfilePushTrigger trigger;

    /** 本次已写入库的档案字段 */
    private List<String> persistedInDatabase = new ArrayList<>();

    /** 处理完成时是否认为设备在线（可尝试下行） */
    private boolean deviceOnline;

    /** 是否已发起 RSocket 下行 */
    private boolean pushAttempted;

    /** 下行报文包含的档案项（与 UpdateConfigRequest 一致） */
    private List<String> notifiedDevicePayloadKeys = new ArrayList<>();

    public static List<String> profileDownlinkKeys() {
        return PROFILE_DOWNLINK_KEYS;
    }

    public static PushConfigResult success(String uuid) {
        PushConfigResult result = new PushConfigResult();
        result.success = true;
        result.code = 200;
        result.msg = "推送成功";
        result.desc = "档案已推送并应用到设备";
        result.uuid = uuid;
        return result;
    }

    public static PushConfigResult offline(String uuid) {
        PushConfigResult result = new PushConfigResult();
        result.success = true;
        result.code = 201;
        result.msg = "设备离线";
        result.desc = "档案已保存，设备上线后自动同步";
        result.uuid = uuid;
        return result;
    }

    public static PushConfigResult timeout(String uuid) {
        PushConfigResult result = new PushConfigResult();
        result.success = false;
        result.code = 408;
        result.msg = "推送超时";
        result.desc = "设备未在指定时间内响应";
        result.uuid = uuid;
        return result;
    }

    public static PushConfigResult rejected(String uuid) {
        PushConfigResult result = new PushConfigResult();
        result.success = false;
        result.code = 409;
        result.msg = "设备拒绝";
        result.desc = "设备拒绝应用档案（版本冲突或其他原因）";
        result.uuid = uuid;
        return result;
    }

    public static PushConfigResult notFound(String uuid) {
        PushConfigResult result = new PushConfigResult();
        result.success = false;
        result.code = 404;
        result.msg = "设备不存在";
        result.desc = "数据库中未找到该设备";
        result.uuid = uuid;
        return result;
    }

    public static PushConfigResult error(String uuid, String errorMsg) {
        PushConfigResult result = new PushConfigResult();
        result.success = false;
        result.code = 500;
        result.msg = "推送失败";
        result.desc = errorMsg != null ? errorMsg : "推送过程中发生异常";
        result.uuid = uuid;
        return result;
    }

    public DeviceProfilePushResultVo toVo() {
        DeviceProfilePushResultVo vo = new DeviceProfilePushResultVo();
        vo.setTrigger(trigger == null ? null : trigger.name());
        vo.setUuid(uuid);
        vo.setSuccess(success);
        vo.setOutcomeCode(code);
        vo.setMsg(msg);
        vo.setDesc(desc);
        vo.setPersistedDatabaseFields(
                persistedInDatabase == null ? new ArrayList<>() : new ArrayList<>(persistedInDatabase));
        vo.setDeviceOnline(deviceOnline);
        vo.setPushAttempted(pushAttempted);
        List<String> keys = notifiedDevicePayloadKeys == null ? List.of() : notifiedDevicePayloadKeys;
        vo.setDevicePayloadFields(new ArrayList<>(keys));
        vo.setDevicePayloadDescriptions(keys.stream().map(DeviceProfilePushResultVo::describePayloadKey).toList());
        return vo;
    }

    public R<DeviceProfilePushResultVo> toR() {
        DeviceProfilePushResultVo data = toVo();
        if (success && code == 200) {
            return R.success(data, topLevelSuccessMsg(200));
        }
        if (success && code == 201) {
            return R.success(data, topLevelSuccessMsg(201));
        }
        if (code == 404) {
            return R.fail(404, "设备不存在", data);
        }
        if (code == 408) {
            return R.fail(408, nonBlankDescOrMsg(), data);
        }
        if (code == 409) {
            return R.fail(409, nonBlankDescOrMsg(), data);
        }
        return R.fail(500, topLevelFailDetail(), data);
    }

    private String topLevelSuccessMsg(int c) {
        if (trigger == ProfilePushTrigger.LABORATORY) {
            if (c == 200) {
                return "实验室关联已更新并已通知设备同步档案";
            }
            return "实验室关联已保存，设备上线后通过注册响应同步档案";
        }
        if (c == 200) {
            return "配置推送成功";
        }
        return "配置已保存，设备离线，上线后自动同步";
    }

    private String topLevelFailDetail() {
        if (trigger == ProfilePushTrigger.LABORATORY) {
            return "实验室关联更新失败: " + nonBlankDescOrMsg();
        }
        return "配置推送失败: " + nonBlankDescOrMsg();
    }

    private String nonBlankDescOrMsg() {
        if (desc != null && !desc.isBlank()) {
            return desc;
        }
        return msg != null ? msg : "未知错误";
    }
}
