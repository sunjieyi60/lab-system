/**
 * 
 */
package xyz.jasenon.lab.core.http;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImConst;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.ImSessionContext;
import xyz.jasenon.lab.core.http.session.HttpSession;
import xyz.jasenon.lab.core.packets.Command;
import xyz.jasenon.lab.core.protocol.IProtocolConverter;

/**
 * HTTP协议消息转化包
 * @author WChao
 *
 */
public class HttpConvertPacket implements IProtocolConverter {

	/**
	 * 转HTTP协议响应包;
	 */
	@Override
	public ImPacket RespPacket(byte[] body, Command command, ImChannelContext channelContext) {
		ImSessionContext sessionContext = channelContext.getSessionContext();
		if(sessionContext instanceof HttpSession){
			HttpRequest request = (HttpRequest)channelContext.getAttribute(ImConst.HTTP_REQUEST);
			HttpResponse response = new HttpResponse(request,request.getHttpConfig());
			response.setBody(body, request);
			response.setCommand(command);
			return response;
		}
		return null;
	}

	@Override
	public ImPacket RespPacket(ImPacket imPacket, Command command, ImChannelContext imChannelContext) {
		ImSessionContext sessionContext = imChannelContext.getSessionContext();
		if(sessionContext instanceof HttpSession){
			HttpResponse response = (HttpResponse)imPacket;
			response.setCommand(command);
			return response;
		}
		return null;
	}

	@Override
	public ImPacket ReqPacket(byte[] body, Command command, ImChannelContext channelContext) {
		
		return null;
	}

}
