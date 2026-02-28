/**
 * 
 */
package xyz.jasenon.lab.core.ws;

import java.nio.ByteBuffer;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.ImSessionContext;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.http.HttpRequest;
import xyz.jasenon.lab.core.http.HttpRequestDecoder;
import xyz.jasenon.lab.core.protocol.AbstractProtocol;
import xyz.jasenon.lab.core.protocol.IProtocolConverter;
import xyz.jasenon.lab.core.utils.ImKit;
import xyz.jasenon.lab.core.ws.WsPacket;

/**
 * WebSocket协议判断器
 * @author WChao
 *
 */
public class WsProtocol extends AbstractProtocol {

	@Override
	public String name() {
		return Protocol.WEB_SOCKET;
	}

	public WsProtocol(IProtocolConverter converter){
		super(converter);
	}
	
	@Override
	protected void init(ImChannelContext imChannelContext) {
		imChannelContext.setSessionContext(new WsSessionContext(imChannelContext));
		ImKit.initImClientNode(imChannelContext);
	}

	@Override
	protected boolean validateProtocol(ImSessionContext imSessionContext) throws ImException {
		if(imSessionContext instanceof WsSessionContext) {
			return true;
		}
		return false;
	}

	@Override
	protected boolean validateProtocol(ByteBuffer buffer, ImChannelContext imChannelContext) throws ImException {
		//第一次连接;
		HttpRequest request = HttpRequestDecoder.decode(buffer, imChannelContext,false);
		if(request.getHeaders().get(Http.RequestHeaderKey.Sec_WebSocket_Key) != null)
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean validateProtocol(ImPacket imPacket) throws ImException {
		if(imPacket instanceof WsPacket){
			return true;
		}
		return false;
	}

}
