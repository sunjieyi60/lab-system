package xyz.jasenon.lab.class_time_table.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tio.core.intf.Packet;
import org.tio.utils.resp.Resp;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImConst;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.message.MessageHelper;
import xyz.jasenon.lab.core.packets.*;
import xyz.jasenon.lab.server.JimServerAPI;
import xyz.jasenon.lab.server.config.ImServerConfig;
import xyz.jasenon.lab.server.protocol.ProtocolManager;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class BitmapService {

    private final ImServerConfig config = ImConfig.Global.get();
    private final MessageHelper helper = config.getMessageHelper();

    public R pushBitmapToClassTimeTable(MultipartFile file, String faceName, List<String> uuids, List<String> groupIds){

        try {
            byte[] bytes = file.getBytes();

            BitmapBody bitmapBody = new BitmapBody(bytes, faceName);
            RespBody respBody = new RespBody(Command.COMMAND_BITMAP_PUSH_REQ).setData(bitmapBody);
            boolean isGroupSend = Objects.nonNull(groupIds) && groupIds.isEmpty();
            if (isGroupSend){
                for (String groupId : groupIds){
                    // 获取所有在线的班牌  暂时不知道离线如何处理 这东西上线推送也不太合适
                    Group group = helper.getGroupClassTimeTables(groupId, ImConst.ONLINE);
                    List<ClassTimeTable> classTimeTables = group.getClassTimeTables();

                    for (ClassTimeTable classTimeTable : classTimeTables){
                        ImChannelContext channelContext = JimServerAPI.getByUserId(classTimeTable.getUuid()).get(0);
                        if (Objects.isNull(channelContext)){
                            //收集错误数量  汇报路由
                            continue;
                        }

                        ImPacket req = ProtocolManager.Converter.respPacket(respBody, channelContext);
                        Packet resp = JimServerAPI.synSend(channelContext, req);
                    }
                }

                return null;
            }

            for()

        } catch (IOException e) {
            // 处理这里的异常  打印日志
            throw new RuntimeException(e);
        }

    }


}
