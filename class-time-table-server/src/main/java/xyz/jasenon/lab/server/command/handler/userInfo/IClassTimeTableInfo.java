package xyz.jasenon.lab.server.command.handler.userInfo;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.core.packets.ClassTimeTableReqBody;

public interface IClassTimeTableInfo {
    /**
     * 获取用户信息接口
     *
     * @param classTimeTableReqBody
     * @param imChannelContext
     * @return
     */
    ClassTimeTable getClassTimeTableInfo(ClassTimeTableReqBody classTimeTableReqBody, ImChannelContext imChannelContext);
}
