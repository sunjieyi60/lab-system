package xyz.jasenon.classtimetable.network;

import android.util.Log;

import org.tio.client.intf.TioClientListener;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;

import java.text.MessageFormat;

public class SmartBoardTioListener implements TioClientListener {
    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
        Log.d("TioListener", MessageFormat.format("isConnected:{0} isReconnect:{1}",isConnected,isReconnect));
    }

    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {

    }

    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {

    }

    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {
        Log.d("TioListener", MessageFormat.format("Packet:{0} send success::{1}",packet, isSentSuccess));
    }

    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {

    }

    @Override
    public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {

    }
}
