package xyz.jasenon.lab.class_time_table.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.class_time_table.Const;
import xyz.jasenon.lab.class_time_table.config.WsServerConfig;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImConst;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.ImStatus;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.http.HttpRequest;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.core.packets.ClassTimeTableStatusType;
import xyz.jasenon.lab.server.JimServerAPI;
import xyz.jasenon.lab.server.processor.handshake.WsHandshakeProcessor;

@Component
@RequiredArgsConstructor
public class LWsHandshakeProcessor extends WsHandshakeProcessor {

    private final WsServerConfig config;
    /**
     * 握手成功后
     * @param packet
     * @param imChannelContext
     * @throws Exception
     * @author Wchao
     */
    @Override
    public void onAfterHandshake(ImPacket packet, ImChannelContext imChannelContext)throws ImException {
        HttpRequest request = (HttpRequest) packet;
        // TODO 重写握手 进行参数校验
        String uuid = (String) request.getParams().get(Const.UUID)[0];
        ClassTimeTable classTimeTable = ClassTimeTable.newBuilder()
                .uuid(uuid)
                .ip(config.server.getIp())
                .terminal(Protocol.WEB_SOCKET)
                .config(config.config)
                .deviceName(uuid)
                .status(ClassTimeTableStatusType.OFFLINE.getStatus())
                .build();
        // 这里会回调ImUserListener onAfterBind方法 持久化设备信息到redis
        JimServerAPI.bindClassTimeTable(imChannelContext, classTimeTable);
    }

}
