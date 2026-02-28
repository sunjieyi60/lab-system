package xyz.jasenon.lab.server.processor.chat;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.packets.ChatBody;
import xyz.jasenon.lab.core.utils.JsonKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author WChao
 * @date 2018年4月3日 下午1:12:30
 */
public class DefaultAsyncChatMessageProcessor extends BaseAsyncChatMessageProcessor {

	private static Logger logger = LoggerFactory.getLogger(DefaultAsyncChatMessageProcessor.class);

	@Override
	public void doProcess(ChatBody chatBody, ImChannelContext imChannelContext){
		logger.info("默认交由业务处理聊天记录示例,用户自己继承BaseAsyncChatMessageProcessor即可:{}", JsonKit.toJSONString(chatBody));
	}
}
