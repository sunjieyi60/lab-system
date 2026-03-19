package xyz.jasenon.lab.server.helper.redis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.cache.redis.RedisCache;
import xyz.jasenon.lab.core.cache.redis.RedisCacheManager;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.listener.AbstractImStoreBindListener;
import xyz.jasenon.lab.core.message.MessageHelper;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.core.packets.ClassTimeTableStatusType;
import xyz.jasenon.lab.core.packets.Group;
import xyz.jasenon.lab.server.config.ImServerConfig;

import java.util.List;
import java.util.Objects;

/**
 * 消息持久化绑定监听器（电子班牌版本）
 * 支持班牌设备的分组绑定/解绑、在线状态管理
 * 
 * @author WChao
 * @author Jasenon_ce (重构: 将User概念替换为ClassTimeTable班牌实体)
 * @date 2018年4月8日 下午4:12:31
 */
public class RedisImStoreBindListener extends AbstractImStoreBindListener {

	private static Logger logger = LoggerFactory.getLogger(RedisImStoreBindListener.class);

	private static final String SUFFIX = ":";
	
	public RedisImStoreBindListener(ImConfig imConfig, MessageHelper messageHelper){
		super(imConfig, messageHelper);
	}
	
	@Override
	public void onAfterGroupBind(ImChannelContext imChannelContext, Group group) throws ImException {
		if(!isStore()) {
			return;
		}
		initGroupClassTimeTables(group, imChannelContext);
	}

	@Override
	public void onAfterGroupUnbind(ImChannelContext imChannelContext, Group group) throws ImException {
		if(!isStore()) {
			return;
		}
		String uuid = imChannelContext.getUserId();
		String groupId = group.getGroupId();
		//移除群组中的班牌;
		RedisCacheManager.getCache(GROUP).listRemove(groupId + SUFFIX + USER, uuid);
		//移除班牌所属的群组;
		RedisCacheManager.getCache(USER).listRemove(uuid + SUFFIX + GROUP, groupId);
		//移除群组离线消息
		RedisCacheManager.getCache(PUSH).remove(GROUP + SUFFIX + groupId + SUFFIX + uuid);
	}

	@Override
	public void onAfterClassTimeTableBind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException {
		if(!isStore() || Objects.isNull(classTimeTable)) {
			return;
		}
		classTimeTable.setStatus(ClassTimeTableStatusType.ONLINE.getStatus());
		this.messageHelper.updateClassTimeTableTerminal(classTimeTable);
		initClassTimeTableInfo(classTimeTable);
	}

	@Override
	public void onAfterClassTimeTableUnbind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException {
		if(!isStore() || Objects.isNull(classTimeTable)) {
			return;
		}
		classTimeTable.setStatus(ClassTimeTableStatusType.OFFLINE.getStatus());
		this.messageHelper.updateClassTimeTableTerminal(classTimeTable);
	}

	/**
	 * 初始化群组班牌成员;
	 * @param group 群组对象
	 * @param imChannelContext 通道上下文
	 */
	public void initGroupClassTimeTables(Group group, ImChannelContext imChannelContext){
		String groupId = group.getGroupId();
		if(!isStore()) {
			return;
		}
		String uuid = imChannelContext.getUserId();
		if(StringUtils.isEmpty(groupId) || StringUtils.isEmpty(uuid)) {
			return;
		}
		String groupClassTimeTableKey = groupId + SUFFIX + USER;
		RedisCache groupCache = RedisCacheManager.getCache(GROUP);
		List<String> classTimeTables = groupCache.listGetAll(groupClassTimeTableKey);
		if(!classTimeTables.contains(uuid)){
			groupCache.listPushTail(groupClassTimeTableKey, uuid);
		}
		initClassTimeTableGroups(uuid, groupId);
		// 缓存群组信息
		groupCache.put(groupId + SUFFIX + INFO, group);
	}

	/**
	 * 初始化班牌拥有的群组列表;
	 * @param uuid 班牌UUID
	 * @param groupId 群组ID
	 */
	public void initClassTimeTableGroups(String uuid, String groupId){
		if(!isStore()) {
			return;
		}
		if(StringUtils.isEmpty(groupId) || StringUtils.isEmpty(uuid)) {
			return;
		}
		List<String> groups = RedisCacheManager.getCache(USER).listGetAll(uuid + SUFFIX + GROUP);
		if(groups.contains(groupId)) return;
		RedisCacheManager.getCache(USER).listPushTail(uuid + SUFFIX + GROUP, groupId);
	}

	/**
	 * 初始化班牌终端协议类型;
	 * @param classTimeTable 班牌实体
	 */
	public void initClassTimeTableInfo(ClassTimeTable classTimeTable){
		if(!isStore() || classTimeTable == null) {
			return;
		}
		String uuid = classTimeTable.getUuid();
		if(StringUtils.isEmpty(uuid)) {
			return;
		}
		RedisCache userCache = RedisCacheManager.getCache(USER);
		userCache.put(uuid + SUFFIX + INFO, classTimeTable.clone());
		// 班牌实体不再存储groups列表，群组关系通过Redis维护
	}

	/**
	 * 是否开启持久化;
	 * @return true:已开启, false:未开启
	 */
	public boolean isStore(){
		ImServerConfig imServerConfig = ImServerConfig.Global.get();
		return ImServerConfig.ON.equals(imServerConfig.getIsStore());
	}

	static{
		RedisCacheManager.register(USER, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(GROUP, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(STORE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(PUSH, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

}
