/**
 * 
 */
package xyz.jasenon.lab.core.tcp;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.ImSessionContext;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.protocol.AbstractProtocol;
import xyz.jasenon.lab.core.protocol.IProtocolConverter;
import xyz.jasenon.lab.core.utils.ImKit;

import java.nio.ByteBuffer;

/**
 * @desc Tcp协议校验器
 * @author WChao
 * @date 2018-05-01
 */
public class TcpProtocol extends AbstractProtocol {

	public TcpProtocol(IProtocolConverter converter){
		super(converter);
	}

	@Override
	public String name() {
		return Protocol.TCP;
	}

	@Override
	protected void init(ImChannelContext imChannelContext) {
		imChannelContext.setSessionContext(new TcpSessionContext(imChannelContext));
		ImKit.initImClientNode(imChannelContext);
	}

	@Override
	public boolean validateProtocol(ImSessionContext imSessionContext) throws ImException {
		if(imSessionContext instanceof TcpSessionContext){
			return true;
		}
		return false;
	}

	@Override
	public boolean validateProtocol(ByteBuffer buffer, ImChannelContext imChannelContext) throws ImException {
		//获取第一个字节协议版本号,TCP协议;
		if(buffer.get() == Protocol.VERSION){
			return true;
		}
		return false;
	}

	@Override
	public boolean validateProtocol(ImPacket imPacket) throws ImException {
		if(imPacket instanceof TcpPacket){
			return true;
		}
		return false;
	}

}
