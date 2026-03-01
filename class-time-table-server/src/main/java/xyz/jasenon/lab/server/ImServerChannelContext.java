package xyz.jasenon.lab.server;

import org.tio.core.ChannelContext;
import org.tio.utils.thread.pool.AbstractQueueRunnable;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.server.protocol.AbstractProtocolHandler;

/**
 * @ClassName ImServerChannelContext
 * @Description 服务端通道上下文
 * @Author WChao
 * @Date 2020/1/5 23:56
 * @Version 1.0
 **/
public class ImServerChannelContext extends ImChannelContext {

    protected AbstractQueueRunnable msgQue;

    protected AbstractProtocolHandler protocolHandler;

    public ImServerChannelContext(ImConfig imConfig, ChannelContext tioChannelContext) {
        super(imConfig, tioChannelContext);
    }

    public AbstractQueueRunnable getMsgQue() {
        return msgQue;
    }

    public void setMsgQue(AbstractQueueRunnable msgQue) {
        this.msgQue = msgQue;
    }

    public AbstractProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    public void setProtocolHandler(AbstractProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }

}
