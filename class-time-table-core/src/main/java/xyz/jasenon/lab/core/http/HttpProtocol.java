/**
 * 
 */
package xyz.jasenon.lab.core.http;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.ImSessionContext;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.http.session.HttpSession;
import xyz.jasenon.lab.core.protocol.AbstractProtocol;
import xyz.jasenon.lab.core.protocol.IProtocolConverter;
import xyz.jasenon.lab.core.utils.ImKit;

import java.nio.ByteBuffer;

/**
 *
 * @desc Http协议校验器
 * @author WChao
 * @date 2018-05-01
 */
public class HttpProtocol extends AbstractProtocol {

	@Override
	public String name() {
		return Protocol.HTTP;
	}

	public HttpProtocol(IProtocolConverter protocolConverter){
		super(protocolConverter);
	}

	@Override
	protected void init(ImChannelContext imChannelContext) {
		imChannelContext.setSessionContext(new HttpSession(imChannelContext));
		ImKit.initImClientNode(imChannelContext);
	}

	@Override
	public boolean validateProtocol(ImSessionContext imSessionContext) throws ImException {
		if(imSessionContext instanceof HttpSession) {
			return true;
		}
		return false;
	}

	@Override
	public boolean validateProtocol(ByteBuffer buffer, ImChannelContext imChannelContext) throws ImException {
		HttpRequest request = HttpRequestDecoder.decode(buffer, imChannelContext,false);
		if(request.getHeaders().get(Http.RequestHeaderKey.Sec_WebSocket_Key) == null)
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean validateProtocol(ImPacket imPacket) throws ImException {
		if(imPacket instanceof HttpPacket){
			return true;
		}
		return false;
	}

}
