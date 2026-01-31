package xyz.jasenon.lab.tio.client;

import org.tio.client.ClientChannelContext;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import org.tio.client.intf.TioClientListener;

/**
 * 客户端连接/断开等回调。
 */
public class SmartBoardClientListener implements TioClientListener {


    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean b, boolean b1) throws Exception {

    }

    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {
    }

    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {
    }

    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {
    }

    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long l) throws Exception {

    }

    @Override
    public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {
        System.out.println("[断开] " + channelContext.getClientNode() + (remark != null ? " " + remark : ""));
    }
}
