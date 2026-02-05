package xyz.jasenon.lab.class_time_table.t_io.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.intf.Packet;
import org.tio.server.intf.TioServerListener;
import xyz.jasenon.lab.class_time_table.service.DeviceSessionService;
import xyz.jasenon.lab.class_time_table.service.DeviceService;

/**
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SmartBoardTioListener implements TioServerListener {
    
    private final DeviceSessionService deviceSessionService;
    private final DeviceService deviceService;
    @Override
    public boolean onHeartbeatTimeout(ChannelContext channelContext, Long aLong, int i) {
        return false;
    }

    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean b, boolean b1) throws Exception {

    }

    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int i) throws Exception {

    }

    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int i) throws Exception {

    }

    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean b) throws Exception {

    }

    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long l) throws Exception {

    }

    @Override
    public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String s, boolean b) throws Exception {
        // 设备断开连接时，解绑设备与通道
        String deviceId = deviceSessionService.getDeviceIdByChannel(channelContext);
        if (deviceId != null) {
            // 解绑设备与通道
            deviceSessionService.unbindDeviceChannel(channelContext);
            
            // 更新设备离线状态
            deviceService.updateDeviceOfflineStatus(deviceId);
            
            log.info("设备{}已断开连接，原因: {}", deviceId, s != null ? s : "未知");
        } else {
            log.debug("未注册的设备断开连接: {}", channelContext.getClientNode());
        }
    }
}
