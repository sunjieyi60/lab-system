package xyz.jasenon.lab.server.processor.group;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.packets.Group;
import xyz.jasenon.lab.core.packets.JoinGroupRespBody;
import xyz.jasenon.lab.server.processor.SingleProtocolCmdProcessor;
/**
 * @author ensheng
 */
public interface GroupCmdProcessor extends SingleProtocolCmdProcessor {
    /**
     * 加入群组处理
     * @param joinGroup
     * @param imChannelContext
     * @return
     */
    JoinGroupRespBody join(Group joinGroup, ImChannelContext imChannelContext);
}
