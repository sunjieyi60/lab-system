package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.*;
import xyz.jasenon.rsocket.core.model.Config;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 心跳请求/响应 - 继承 Message
 * 
 * 简化设计：直接作为 Message 的子类，无需通过 data 字段包装
 * @author Jasenon_ce
 */
@Getter
@Setter
public class Heartbeat extends Message implements ServerSend, ClientSend, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 班牌唯一编号
     */
    private String uuid;

    /**
     * 心跳间隔（秒）
     */
    private Integer interval;

    /**
     * 是否有配置更新
     */
    private Boolean configUpdated;

    /**
     * 新配置（如果有更新）
     */
    private Config newConfig;

    /**
     * 创建心跳请求
     */
    public static Heartbeat request(String uuid, Integer interval) {
        Heartbeat heartbeat = new Heartbeat();
        // client -> server 使用 route
        heartbeat.setRoute(Const.Route.DEVICE_HEARTBEAT);
        heartbeat.setUuid(uuid);
        heartbeat.setInterval(interval);
        heartbeat.setStatus(Status.C10000);
        heartbeat.setTimestamp(Instant.now());
        return heartbeat;
    }

    /**
     * 创建带配置更新的心跳响应
     */
    public static Heartbeat responseWithConfig(String uuid, Config newConfig) {
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setUuid(uuid);
        heartbeat.setConfigUpdated(true);
        heartbeat.setNewConfig(newConfig);
        heartbeat.setTimestamp(Instant.now());
        return heartbeat;
    }

    @Override
    public String route() {
        return super.getRoute();
    }

    @Override
    public Command command() {
        return super.getCommand();
    }
}
