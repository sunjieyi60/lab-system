package xyz.jasenon.lab.class_time_table.t_io.service;

import org.tio.core.ChannelContext;
import xyz.jasenon.lab.class_time_table.t_io.protocol.QosManager;
import xyz.jasenon.lab.class_time_table.t_io.protocol.SmartBoardPacket;

/**
* @author Jasenon_ce
* @date 2026/2/3 
*/

public abstract class Handler {

    private final QosManager qosManager;

    public Handler(QosManager qosManager) {
        this.qosManager = qosManager;
    }

    abstract void register();

    public void handle(SmartBoardPacket packet, ChannelContext channelContext){
        qosManager.handleAck(packet.getSeqId(), channelContext);
        if (packet.requiresAck()){
            qosManager.handleClientAckRequire(packet, channelContext);
        }
        qosManager.qosStart(bizPacket(packet,channelContext), channelContext);
    }

    abstract SmartBoardPacket bizPacket(SmartBoardPacket packet,ChannelContext ctx);

}
