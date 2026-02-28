package xyz.jasenon.lab.server.protocol;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.packets.Message;
import xyz.jasenon.lab.server.processor.ProtocolCmdProcessor;

/**
 * @author WChao
 * @Desc
 * @date 2020-05-02 16:23
 */
public abstract class AbstractProtocolCmdProcessor implements ProtocolCmdProcessor {

    @Override
    public void process(ImChannelContext imChannelContext, Message message) {

    }
}
