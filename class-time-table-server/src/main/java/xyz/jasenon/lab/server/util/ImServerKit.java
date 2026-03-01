/**
 * 
 */
package xyz.jasenon.lab.server.util;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.server.JimServerAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * IM工具类;
 * @author WChao
 *
 */
public class ImServerKit {
	
	private static Logger logger = LoggerFactory.getLogger(ImServerKit.class);

	/**
	 * 根据群组获取所有用户;
	 * @param groupId 群组ID
	 * @return 群组用户集合列表
	 */
	public static List<ClassTimeTable> getUsersByGroup(String groupId){
		List<ImChannelContext> channelContexts = JimServerAPI.getByGroup(groupId);
		List<ClassTimeTable> classTimeTables = Lists.newArrayList();
		if(CollectionUtils.isEmpty(channelContexts)){
			return classTimeTables;
		}
		Map<String,ClassTimeTable> userMap = new HashMap<>();
		channelContexts.forEach(imChannelContext -> {
			ClassTimeTable classTimeTable = imChannelContext.getSessionContext().getImClientNode().getClassTimeTable();
			if(Objects.nonNull(classTimeTable) && Objects.isNull(userMap.get(classTimeTable.getUuid()))){
				userMap.put(classTimeTable.getUuid(), classTimeTable);
				classTimeTables.add(classTimeTable);
			}
		});
		return classTimeTables;
	}

	/**
	 * 根据班牌ID获取班牌信息(一个用户ID会有多端通道,默认取第一个)
	 * @param uuid 用户ID
	 * @return classTimeTable信息
	 */
	public static ClassTimeTable getClassTimeTable(String uuid){
		List<ImChannelContext> imChannelContexts = JimServerAPI.getByUserId(uuid);
		if(CollectionUtils.isEmpty(imChannelContexts)) {
			return null;
		}
		return imChannelContexts.get(0).getSessionContext().getImClientNode().getClassTimeTable();
	}

}
