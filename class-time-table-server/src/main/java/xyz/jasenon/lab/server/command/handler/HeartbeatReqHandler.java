package xyz.jasenon.lab.server.command.handler;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.packets.Command;
import xyz.jasenon.lab.core.packets.HeartbeatBody;
import xyz.jasenon.lab.core.packets.RespBody;
import xyz.jasenon.lab.server.command.AbstractCmdHandler;
import xyz.jasenon.lab.server.protocol.ProtocolManager;

/**
 *
 */
public class HeartbeatReqHandler extends AbstractCmdHandler
{
	@Override
	public ImPacket handler(ImPacket packet, ImChannelContext channelContext) throws ImException
	{
		RespBody heartbeatBody = new RespBody(Command.COMMAND_HEARTBEAT_REQ).setData(new HeartbeatBody(Protocol.HEARTBEAT_BYTE));
		ImPacket heartbeatPacket = ProtocolManager.Converter.respPacket(heartbeatBody,channelContext);
		return heartbeatPacket;
	}

	@Override
	public Command command() {
		return Command.COMMAND_HEARTBEAT_REQ;
	}
}
