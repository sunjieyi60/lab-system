package xyz.jasenon.lab.server.command.handler.userInfo;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.message.MessageHelper;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.core.packets.ClassTimeTableReqBody;
import xyz.jasenon.lab.server.config.ImServerConfig;

/**
 * 持久化获取用户信息处理
 */
public class PersistentClassTimeTableInfo implements IClassTimeTableInfo {

    @Override
    public ClassTimeTable getClassTimeTableInfo(ClassTimeTableReqBody classTimeTableReqBody, ImChannelContext imChannelContext) {
        ImServerConfig imServerConfig = ImConfig.Global.get();
        String uuid = classTimeTableReqBody.getUuid();
        Integer type = classTimeTableReqBody.getType();
        //消息持久化助手;
        MessageHelper messageHelper = imServerConfig.getMessageHelper();
        ClassTimeTable classTimeTable = messageHelper.getClassTimeTableByType(uuid, type);
        return classTimeTable;
    }

}
