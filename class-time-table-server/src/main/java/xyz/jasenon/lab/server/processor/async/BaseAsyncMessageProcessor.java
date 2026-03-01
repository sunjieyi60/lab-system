package xyz.jasenon.lab.server.processor.async;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.message.MessageHelper;
import xyz.jasenon.lab.core.packets.Message;
import xyz.jasenon.lab.core.packets.RespBody;
import xyz.jasenon.lab.server.JimServerAPI;
import xyz.jasenon.lab.server.config.ImServerConfig;
import xyz.jasenon.lab.server.processor.SingleProtocolCmdProcessor;
import xyz.jasenon.lab.server.protocol.ProtocolManager;
import xyz.jasenon.lab.server.util.ChatKit;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通用异步消息处理器基类
 * 
 * 设计要点：
 * 1. 泛型支持：Req-请求类型, Resp-响应类型
 * 2. 模板方法：定义处理流程，子类只关注业务逻辑
 * 3. 前置/后置增强：支持AOP式扩展
 * 4. 异步持久化：不阻塞业务响应
 * 
 * @param <Req>  请求消息类型
 * @param <Resp> 响应消息类型
 * @author Jasenon_ce
 * @date 2026/3/1
 */
@Slf4j
public abstract class BaseAsyncMessageProcessor<Req extends Message, Resp extends RespBody> 
        implements SingleProtocolCmdProcessor {

    private final Boolean STORE;

    public BaseAsyncMessageProcessor(Boolean store) {
        this.STORE = store;
    }

    protected ImServerConfig imServerConfig = ImConfig.Global.get();

    /**
     * 异步执行器
     */
    protected static final ExecutorService ASYNC_EXECUTOR = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2,
        r -> {
            Thread t = new Thread(r, "CTT-Async-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        }
    );

    /**
     * 主处理流程（模板方法）
     */
    @Override
    public void process(ImChannelContext ctx, Message message) {
        Req req = (Req) message;

        // 1. 前置处理
        if (!preProcess(ctx, req)) {
            log.warn("[{}] 前置处理未通过", getClass().getSimpleName());
            return;
        }

        // 2. 业务处理
        Resp resp = doProcess(ctx, req);

        // 3. 发送响应
        if (resp != null) {
            sendResponse(ctx, resp);
        }

        // 4. 异步持久化
        if (STORE) {
            CompletableFuture.runAsync(() -> persist(ctx, req, resp), ASYNC_EXECUTOR)
                .exceptionally(e -> {
                    log.error("[{}] 持久化失败", getClass().getSimpleName(), e);
                    return null;
                });
        }

        // 5. 后置处理
        afterProcess(ctx, req, resp);
    }

    /**
     * 核心业务处理 - 子类必须实现
     * 
     * @param ctx 通道上下文
     * @param req 请求消息
     * @return 响应消息，可为null表示无需响应
     */
    protected abstract Resp doProcess(ImChannelContext ctx, Req req);

    /**
     * 数据持久化 - 子类必须实现
     * 
     * @param ctx  通道上下文
     * @param req  请求消息
     * @param resp 响应消息
     */
    protected abstract void persist(ImChannelContext ctx, Req req, Resp resp);

    /**
     * 前置处理 - 子类可重写
     * 默认：校验 + 在线检查
     * 
     * @param ctx 通道上下文
     * @param req 请求消息
     * @return true-继续处理, false-终止
     */
    protected boolean preProcess(ImChannelContext ctx, Req req) {
        // 参数校验
        if (!validate(req)) {
            log.warn("参数校验失败");
            return false;
        }
        // 在线检查
        if (requireOnline() && !isOnline(ctx)) {
            log.warn("班牌不在线");
            return false;
        }
        return true;
    }

    /**
     * 后置处理 - 子类可重写
     * 用于：事件发布、MQ通知、日志记录等
     * 
     * @param ctx  通道上下文
     * @param req  请求消息
     * @param resp 响应消息
     */
    protected void afterProcess(ImChannelContext ctx, Req req, Resp resp) {
        // 默认空实现，子类可重写
    }

    /**
     * 参数校验 - 子类可重写
     * 
     * @param req 请求消息
     * @return true-校验通过
     */
    protected boolean validate(Req req) {
        return req != null;
    }

    /**
     * 是否需要班牌在线 - 子类可重写
     * 
     * @return true-需要在线（默认false）
     */
    protected boolean requireOnline() {
        return false;
    }

    /**
     * 检查班牌是否在线
     */
    protected boolean isOnline(ImChannelContext ctx) {
        String uuid = ctx.getUserId();
        if (Objects.isNull(uuid)) {
            return false;
        }
        boolean isStore = ImServerConfig.ON.equals(imServerConfig.getIsStore());
        return ChatKit.isOnline(uuid, isStore);
    }

    /**
     * 发送响应
     */
    @SneakyThrows
    protected void sendResponse(ImChannelContext ctx, Resp resp) {
        if (ctx != null && resp != null) {
            ImPacket packet = ProtocolManager.Converter.respPacket(resp,ctx);
            JimServerAPI.send(ctx,packet);
        }
    }


    /**
     * 获取MessageHelper
     */
    protected MessageHelper getMessageHelper() {
        return imServerConfig.getMessageHelper();
    }
}
