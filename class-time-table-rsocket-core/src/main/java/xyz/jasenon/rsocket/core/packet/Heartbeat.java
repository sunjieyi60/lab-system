package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.*;
import xyz.jasenon.rsocket.core.model.Config;

import java.io.Serial;
import java.io.Serializable;
/**
 * 心跳请求/响应 - 继承 Message
 * 
 * 简化设计：直接作为 Message 的子类，无需通过 data 字段包装
 * @author Jasenon_ce
 */
@Getter
@Setter
public class Heartbeat extends Message implements ServerSend<Heartbeat>, ClientSend, Serializable {

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
     * 创建心跳请求
     */
    public static Heartbeat request(String uuid, Integer interval) {
        Heartbeat heartbeat = new Heartbeat();
        // client -> server 使用 route
        heartbeat.setRoute(Const.Route.DEVICE_HEARTBEAT);
        heartbeat.setUuid(uuid);
        heartbeat.setInterval(interval);
        heartbeat.setStatus(Status.C10000);
        heartbeat.setTimestamp(System.currentTimeMillis());
        return heartbeat;
    }

    /**
     * 创建带配置更新的心跳响应
     */
    public static Heartbeat responseWithConfig(String uuid) {
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setUuid(uuid);
        heartbeat.setTimestamp(System.currentTimeMillis());
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

    public Heartbeat(){
        init(this);
    }
}
