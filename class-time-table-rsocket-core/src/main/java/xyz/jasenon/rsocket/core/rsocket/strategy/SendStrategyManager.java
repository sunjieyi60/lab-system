package xyz.jasenon.rsocket.core.rsocket.strategy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 发送策略管理器（单例）
 * 
 * 管理所有发送策略，根据消息类型选择对应的策略执行
 * 提供类型安全的发送方法
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SendStrategyManager {
    
    /**
     * 策略映射表
     */
    private final Map<Message.Type, SendStrategy> strategies = new EnumMap<>(Message.Type.class);
    
    /**
     * 自动注入所有策略
     */
    private final List<SendStrategy> strategyList;
    
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
     * 执行发送（基础方法）
     * 
     * @param message 消息
     * @param requester RSocket 连接
     * @param route 路由路径
     * @return 响应消息（FireAndForget 返回 Mono.empty()）
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
     * 执行发送（类型安全版本）
     * 
     * @param message 请求消息
     * @param responseType 响应数据类型
     * @param requester RSocket 连接
     * @param route 路由路径
     * @return 类型安全的响应消息
     */
    public <R> Mono<Message<R>> send(Message<?> message, Class<R> responseType, 
                                      RSocketRequester requester, String route) {
        SendStrategy strategy = getStrategy(message.getType());
        if (strategy == null) {
            return Mono.error(new UnsupportedOperationException(
                "不支持的消息类型: " + message.getType()));
        }
        return strategy.send(message, responseType, requester, route);
    }
    
    /**
     * 执行发送（通过 MessageAdaptor，自动类型匹配）
     * 
     * @param adaptor 消息适配器（包含请求和响应类型）
     * @param requester RSocket 连接
     * @return 类型安全的响应消息
     */
    public <T, R> Mono<Message<R>> send(MessageAdaptor<T, R> adaptor, RSocketRequester requester) {
        Message<T> message = adaptor.adaptor();
        Class<R> responseType = adaptor.getResponseType();
        return send(message, responseType, requester, message.getRoute());
    }
    
    /**
     * Fire-and-Forget 发送
     * 
     * @param message 消息
     * @param requester RSocket 连接
     * @param route 路由路径
     * @return Mono<Void>
     */
    public Mono<Void> sendAndForget(Message<?> message, RSocketRequester requester, String route) {
        SendStrategy strategy = getStrategy(Message.Type.FIRE_AND_FORGET);
        if (strategy == null) {
            return Mono.error(new UnsupportedOperationException("FireAndForget 策略未注册"));
        }
        return strategy.sendAndForget(message, requester, route);
    }
    
    /**
     * Fire-and-Forget 发送（通过 MessageAdaptor）
     * 
     * @param adaptor 消息适配器
     * @param requester RSocket 连接
     * @return Mono<Void>
     */
    public Mono<Void> sendAndForget(MessageAdaptor<?, ?> adaptor, RSocketRequester requester) {
        Message<?> message = adaptor.adaptor();
        return sendAndForget(message, requester, message.getRoute());
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
