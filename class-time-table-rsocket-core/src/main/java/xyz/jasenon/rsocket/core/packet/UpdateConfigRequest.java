package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.model.Config;



/**
 * 更新配置请求
 * 
 * 服务器向设备推送新配置
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class UpdateConfigRequest extends Message implements ServerSend {

    private static final long serialVersionUID = 1L;

    /**
     * 新配置
     */
    private Config config;

    /**
     * 是否立即生效
     */
    private Boolean immediate;

    /**
     * 配置版本号（用于冲突检测）
     */
    private Long version;

    /**
     * 服务端登记的关联实验室（与设备表 laboratory_id 一致，设备端可据此对齐档案）
     */
    private Long laboratoryId;

    /**
     * 请求时间
     */
    private Long requestTime;

    /**
     * 创建配置更新请求 Message
     */
    public static UpdateConfigRequest create(Config config, Boolean immediate, Long version) {
        return create(config, immediate, version, null);
    }

    /**
     * 创建配置更新请求（含服务端登记的实验室 ID）
     */
    public static UpdateConfigRequest create(Config config, Boolean immediate, Long version, Long laboratoryId) {
        UpdateConfigRequest request = new UpdateConfigRequest();
        // client -> server 使用 route
        request.setRoute(Const.Route.DEVICE_CONFIG_UPDATE);
        request.setStatus(Status.C10000);
        request.setConfig(config);
        request.setImmediate(immediate);
        request.setVersion(version);
        request.setLaboratoryId(laboratoryId);
        request.setRequestTime(System.currentTimeMillis());
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    @Override
    public Command command() {
        return Command.UPDATE_CONFIG;
    }
}
