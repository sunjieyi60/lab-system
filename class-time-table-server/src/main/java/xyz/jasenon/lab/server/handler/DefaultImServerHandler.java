package xyz.jasenon.lab.server.handler;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.exception.ImDecodeException;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.server.ImServerChannelContext;
import xyz.jasenon.lab.server.protocol.AbstractProtocolHandler;
import xyz.jasenon.lab.server.protocol.ProtocolManager;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @ClassName DefaultImServerHandler
 * @Description TODO
 * @Author WChao
 * @Date 2020/1/6 2:25
 * @Version 1.0
 **/
public class DefaultImServerHandler implements ImServerHandler {
    /**
     * 处理消息包
     * @param imPacket
     * @param imChannelContext
     * @throws Exception
     */
    @Override
    public void handler(ImPacket imPacket, ImChannelContext imChannelContext) throws ImException {
        ImServerChannelContext imServerChannelContext = (ImServerChannelContext)imChannelContext;
        AbstractProtocolHandler handler = imServerChannelContext.getProtocolHandler();
        if(Objects.isNull(handler)){
            return;
        }
        handler.handler(imPacket, imChannelContext);
    }

    /**
     * 编码
     * @param imPacket
     * @param imConfig
     * @param imChannelContext
     * @return
     */
    @Override
    public ByteBuffer encode(ImPacket imPacket, ImConfig imConfig, ImChannelContext imChannelContext) {
        ImServerChannelContext imServerChannelContext = (ImServerChannelContext)imChannelContext;
        AbstractProtocolHandler handler = imServerChannelContext.getProtocolHandler();
        if(handler != null){
            return handler.encode(imPacket, imConfig, imServerChannelContext);
        }
        return null;
    }

    /**
     * 解码
     * @param buffer
     * @param limit
     * @param position
     * @param readableLength
     * @param imChannelContext
     * @return
     * @throws ImDecodeException
     */
    @Override
    public ImPacket decode(ByteBuffer buffer, int limit, int position, int readableLength, ImChannelContext imChannelContext) throws ImDecodeException {
        ImServerChannelContext imServerChannelContext = (ImServerChannelContext)imChannelContext;
        AbstractProtocolHandler handler = imServerChannelContext.getProtocolHandler();
        if(Objects.isNull(imServerChannelContext.getSessionContext())){
            handler = ProtocolManager.initProtocolHandler(buffer, imServerChannelContext);
        }
        if(handler != null){
            return handler.decode(buffer, limit, position, readableLength, imServerChannelContext);
        }else{
            throw new ImDecodeException("unsupported protocol type, the protocol decoder cannot be found");
        }
    }
}
