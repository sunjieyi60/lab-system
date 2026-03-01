/**
 * 
 */
package xyz.jasenon.lab.server.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.server.JimServerAPI;
import xyz.jasenon.lab.server.config.ImServerConfig;

import java.util.List;

/**
 * IM聊天命令工具类
 * @date 2018-09-05 23:29:30
 * @author WChao
 *
 */
public class ChatKit {
	
	private static Logger log = Logger.getLogger(ChatKit.class);

     /**
      * 判断用户是否在线
      * @param userId 用户ID
	  * @param isStore 是否开启持久化(true:开启,false:未开启)
      * @return
      */
     public static boolean isOnline(String userId , boolean isStore){
		 if(isStore){
			ImServerConfig imServerConfig = ImConfig.Global.get();
			return imServerConfig.getMessageHelper().isOnline(userId);
		 }
    	 List<ImChannelContext> imChannelContexts = JimServerAPI.getByUserId(userId);
    	 if(CollectionUtils.isNotEmpty(imChannelContexts)){
    		 return true;
    	 }
    	 return false;
     }

	/**
	 * 获取双方会话ID
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public static String sessionId(String from, String to) {
		if (from.compareTo(to) <= 0) {
			return from + "-" + to;
		} else {
			return to + "-" + from;
		}
	}
}
