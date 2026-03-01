/**
 * 
 */
package xyz.jasenon.lab.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImSessionContext;
import xyz.jasenon.lab.core.packets.ImClientNode;

import java.util.Objects;

/**
 * IM工具类;
 * @author WChao
 *
 */
public class ImKit {
	
	private static Logger logger = LoggerFactory.getLogger(ImKit.class);

	/**
	 * 设置Client对象到ImSessionContext中
	 * @param channelContext 通道上下文
	 * @return 客户端Node信息
	 * @author: WChao
	 */
	public static ImClientNode initImClientNode(ImChannelContext channelContext) {
		ImSessionContext imSessionContext = channelContext.getSessionContext();
		ImClientNode imClientNode = imSessionContext.getImClientNode();
		if(Objects.nonNull(imClientNode)){
			return imClientNode;
		}
		imClientNode = ImClientNode.newBuilder().id(channelContext.getId()).ip(channelContext.getClientNode().getIp()).port(channelContext.getClientNode().getPort()).build();
		imSessionContext.setImClientNode(imClientNode);
		return imClientNode;
	}

}
