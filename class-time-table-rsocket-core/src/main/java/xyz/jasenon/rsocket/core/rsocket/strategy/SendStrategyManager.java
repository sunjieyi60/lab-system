package xyz.jasenon.rsocket.core.rsocket.strategy;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 发送策略管理器（单例）
 * 
 * 管理所有发送策略，根据消息类型选择对应的策略执行
 */
@Slf4j
@Component
public class SendStrategyManager {
    
    /**
     * 策略映射表
     */
    private final Map<Message.Type, SendStrategy> strategies = new EnumMap<>(Message.Type.class);
    
    /**
     * 自动注入所有策略
     */
    @Autowired
    private List<SendStrategy> strategyList;
    
    /**
     * 初始化策略映射
     */
    @PostConstruct
    public void init() {
        for (SendStrategy strategy : strategyList) {
            strategies.put(strategy.getType(), strategy);
            log.info("注册发送策略: {}", strategy.getType());
        }
    }
    
    /**
     * 获取策略
     * 
     * @param type 消息类型
     * @return 对应的策略，如果不存在返回 null
     */
    public SendStrategy getStrategy(Message.Type type) {
        return strategies.get(type);
    }
    
    /**
     * 执行发送
     * 
     * @param message 消息
     * @param requester RSocket 连接
     * @param route 路由路径
     * @return 响应消息
     */
    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route) {
        SendStrategy strategy = getStrategy(message.getType());
        if (strategy == null) {
            return Mono.error(new UnsupportedOperationException(
                "不支持的消息类型: " + message.getType()));
        }
        return strategy.send(message, requester, route);
    }
    
    /**
     * 检查是否支持该消息类型
     * 
     * @param type 消息类型
     * @return 是否支持
     */
    public boolean supports(Message.Type type) {
        return strategies.containsKey(type);
    }
}
