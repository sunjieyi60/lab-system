/**
 * 
 */
package xyz.jasenon.lab.server.protocol.tcp;

import xyz.jasenon.lab.core.*;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.exception.ImDecodeException;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.packets.Command;
import xyz.jasenon.lab.core.packets.RespBody;
import xyz.jasenon.lab.core.protocol.AbstractProtocol;
import xyz.jasenon.lab.core.tcp.*;
import xyz.jasenon.lab.server.JimServerAPI;
import xyz.jasenon.lab.server.command.AbstractCmdHandler;
import xyz.jasenon.lab.server.command.CommandManager;
import xyz.jasenon.lab.server.config.ImServerConfig;
import xyz.jasenon.lab.server.protocol.AbstractProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * 版本: [1.0]
 * 功能说明: 
 * @author : WChao 创建时间: 2017年8月3日 下午7:44:48
 */
public class TcpProtocolHandler extends AbstractProtocolHandler {
	
	Logger logger = LoggerFactory.getLogger(TcpProtocolHandler.class);

	public TcpProtocolHandler(){
		this.protocol = new TcpProtocol(new TcpConvertPacket());
	}

	public TcpProtocolHandler(AbstractProtocol protocol){
		super(protocol);
	}

	@Override
	public void init(ImServerConfig imServerConfig) {
		logger.info("Socket Protocol initialized");
	}
	@Override
	public ByteBuffer encode(ImPacket imPacket, ImConfig imConfig, ImChannelContext imChannelContext) {
		TcpPacket tcpPacket = (TcpPacket)imPacket;
		return TcpServerEncoder.encode(tcpPacket, imConfig, imChannelContext);
	}

	@Override
	public void handler(ImPacket packet, ImChannelContext imChannelContext)throws ImException {
		TcpPacket tcpPacket = (TcpPacket)packet;
		AbstractCmdHandler cmdHandler = CommandManager.getCommand(tcpPacket.getCommand());
		if(cmdHandler == null){
			ImPacket imPacket = new ImPacket(Command.COMMAND_UNKNOW, new RespBody(Command.COMMAND_UNKNOW,ImStatus.C10017).toByte());
			JimServerAPI.send(imChannelContext, imPacket);
			return;
		}
		ImPacket response = cmdHandler.handler(tcpPacket, imChannelContext);
		if(Objects.nonNull(response) && tcpPacket.getSynSeq() < 1){
			JimServerAPI.send(imChannelContext, response);
		}
	}

	@Override
	public TcpPacket decode(ByteBuffer buffer, int limit, int position, int readableLength, ImChannelContext imChannelContext)throws ImDecodeException {
		TcpPacket tcpPacket = TcpServerDecoder.decode(buffer, imChannelContext);
		return tcpPacket;
	}

}
