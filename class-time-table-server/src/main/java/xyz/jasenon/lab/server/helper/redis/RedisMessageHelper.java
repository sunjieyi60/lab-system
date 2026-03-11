package xyz.jasenon.lab.server.helper.redis;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jasenon.lab.core.cache.redis.JedisTemplate;
import xyz.jasenon.lab.core.cache.redis.RedisCacheManager;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.listener.ImStoreBindListener;
import xyz.jasenon.lab.core.message.AbstractMessageHelper;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.core.packets.ClassTimeTableStatusType;
import xyz.jasenon.lab.core.packets.Group;

import java.util.*;

/**
 * Redis获取持久化+同步消息助手;
 * 支持电子班牌设备管理（分组、在线状态等）
 * 
 * @author WChao
 * @author Jasenon_ce (重构: 将User概念替换为ClassTimeTable班牌实体)
 * @date 2018年4月9日 下午4:39:30
 */
public class RedisMessageHelper extends AbstractMessageHelper {

	private Logger log = LoggerFactory.getLogger(RedisMessageHelper.class);

	private static final String SUFFIX = ":";

	public RedisMessageHelper() {
		this.imConfig = ImConfig.Global.get();
	}

	@Override
	public ImStoreBindListener getBindListener() {
		return new RedisImStoreBindListener(imConfig, this);
	}

	@Override
	public boolean isOnline(String uuid) {
		try {
			String keyPattern = USER + SUFFIX + uuid + SUFFIX + TERMINAL;
			Set<String> terminalKeys = JedisTemplate.me().keys(keyPattern);
			if (CollectionUtils.isEmpty(terminalKeys)) {
				return false;
			}
			Iterator<String> terminalKeyIterator = terminalKeys.iterator();
			while (terminalKeyIterator.hasNext()) {
				String terminalKey = terminalKeyIterator.next();
				terminalKey = terminalKey.substring(terminalKey.indexOf(uuid));
				String status = RedisCacheManager.getCache(USER).get(terminalKey, String.class);
				if (ClassTimeTableStatusType.ONLINE.getStatus().equals(status)) {
					return true;
				}
			}
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
		return false;
	}

	@Override
	public List<String> getGroupClassTimeTableIds(String groupId) {
		String groupUserKey = groupId + SUFFIX + USER;
		return RedisCacheManager.getCache(GROUP).listGetAll(groupUserKey);
	}

	@Override
	public void addGroupClassTimeTable(String uuid, String groupId) {
		List<String> uuids = RedisCacheManager.getCache(GROUP).listGetAll(groupId);
		if (uuids.contains(uuid)) {
			return;
		}
		RedisCacheManager.getCache(GROUP).listPushTail(groupId, uuid);
	}

	@Override
	public void removeGroupClassTimeTable(String uuid, String groupId) {
		RedisCacheManager.getCache(GROUP).listRemove(groupId, uuid);
	}

	/**
	 * 获取群组所有班牌成员信息
	 * @param groupId 群组ID
	 * @param type 筛选类型(0:所有在线班牌,1:所有离线班牌,2:所有班牌[在线+离线])
	 * @return Group 群组对象
	 */
	@Override
	public Group getGroupClassTimeTables(String groupId, Integer type) {
		if (Objects.isNull(groupId) || Objects.isNull(type)) {
			log.warn("group:{} or type:{} is null", groupId, type);
			return null;
		}
		Group group = RedisCacheManager.getCache(GROUP).get(groupId + SUFFIX + INFO, Group.class);
		if (Objects.isNull(group)) {
			return null;
		}
		List<String> uuids = this.getGroupClassTimeTableIds(groupId);
		if (CollectionUtils.isEmpty(uuids)) {
			return null;
		}
		List<ClassTimeTable> classTimeTables = new ArrayList<ClassTimeTable>();
		uuids.forEach(uuid -> {
			ClassTimeTable classTimeTable = getClassTimeTableByType(uuid, type);
			if (Objects.isNull(classTimeTable))
				return;
			validateStatusByType(type, classTimeTables, classTimeTable);
		});
		group.setClassTimeTables(classTimeTables);
		return group;
	}

	/**
	 * 根据类型校验是否组装ClassTimeTable
	 * @param type 筛选类型
	 * @param classTimeTables 班牌列表
	 * @param classTimeTable 班牌实体
	 */
	private void validateStatusByType(Integer type, List<ClassTimeTable> classTimeTables, ClassTimeTable classTimeTable) {
		String status = classTimeTable.getStatus();
		if (ClassTimeTableStatusType.ONLINE.getNumber() == type && ClassTimeTableStatusType.ONLINE.getStatus().equals(status)) {
			classTimeTables.add(classTimeTable);
		} else if (ClassTimeTableStatusType.OFFLINE.getNumber() == type && ClassTimeTableStatusType.OFFLINE.getStatus().equals(status)) {
			classTimeTables.add(classTimeTable);
		} else if (ClassTimeTableStatusType.ALL.getNumber() == type) {
			classTimeTables.add(classTimeTable);
		}
	}

	@Override
	public ClassTimeTable getClassTimeTableByType(String uuid, Integer type) {
		ClassTimeTable classTimeTable = RedisCacheManager.getCache(USER).get(uuid + SUFFIX + INFO, ClassTimeTable.class);
		if (Objects.isNull(classTimeTable)) {
			return null;
		}
		boolean isOnline = this.isOnline(uuid);
		String status = isOnline ? ClassTimeTableStatusType.ONLINE.getStatus() : ClassTimeTableStatusType.OFFLINE.getStatus();
		if (ClassTimeTableStatusType.ONLINE.getNumber() == type && isOnline) {
			classTimeTable.setStatus(status);
			return classTimeTable;
		} else if (ClassTimeTableStatusType.OFFLINE.getNumber() == type && !isOnline) {
			classTimeTable.setStatus(status);
			return classTimeTable;
		} else if (type == ClassTimeTableStatusType.ALL.getNumber()) {
			classTimeTable.setStatus(status);
			return classTimeTable;
		}
		return null;
	}

	@Override
	public Group getRelatedClassTimeTables(String uuid, String relatedGroupId, Integer type) {
		boolean isTrue = Objects.isNull(uuid) || Objects.isNull(relatedGroupId) || Objects.isNull(type);
		if (isTrue) {
			log.warn("uuid:{} or relatedGroupId:{} or type:{} is null");
			return null;
		}
		List<Group> relatedGroups = RedisCacheManager.getCache(USER).get(uuid + SUFFIX + FRIENDS, List.class);
		if (CollectionUtils.isEmpty(relatedGroups)) {
			return null;
		}
		for (Group group : relatedGroups) {
			if (!relatedGroupId.equals(group.getGroupId()))
				continue;
			List<ClassTimeTable> classTimeTables = group.getClassTimeTables();
			if (CollectionUtils.isEmpty(classTimeTables)) {
				return group;
			}
			List<ClassTimeTable> results = new ArrayList<ClassTimeTable>();
			for (ClassTimeTable classTimeTable : classTimeTables) {
				initClassTimeTableStatus(classTimeTable);
				validateStatusByType(type, results, classTimeTable);
			}
			group.setClassTimeTables(results);
			return group;
		}
		return null;
	}

	/**
	 * 初始化班牌在线状态
	 * @param classTimeTable 班牌实体
	 */
	public boolean initClassTimeTableStatus(ClassTimeTable classTimeTable) {
		if (Objects.isNull(classTimeTable) || Objects.isNull(classTimeTable.getUuid())) {
			return false;
		}
		String uuid = classTimeTable.getUuid();
		boolean isOnline = this.isOnline(uuid);
		if (isOnline) {
			classTimeTable.setStatus(ClassTimeTableStatusType.ONLINE.getStatus());
		} else {
			classTimeTable.setStatus(ClassTimeTableStatusType.OFFLINE.getStatus());
		}
		return true;
	}

	/**
	 * 获取所有关联班牌分组成员信息
	 * @param uuid 班牌设备UUID
	 * @param type 筛选类型(0:所有在线班牌,1:所有离线班牌,2:所有班牌[在线+离线])
	 * @return List<Group> 群组列表
	 */
	@Override
	public List<Group> getAllRelatedClassTimeTables(String uuid, Integer type) {
		if (Objects.isNull(uuid)) {
			return null;
		}
		List<JSONObject> relatedJsonArray = RedisCacheManager.getCache(USER).get(uuid + SUFFIX + FRIENDS, List.class);
		if (CollectionUtils.isEmpty(relatedJsonArray)) {
			return null;
		}
		List<Group> relatedGroups = new ArrayList<Group>();
		relatedJsonArray.forEach(groupJson -> {
			Group group = JSONObject.toJavaObject(groupJson, Group.class);
			List<ClassTimeTable> classTimeTables = group.getClassTimeTables();
			if (CollectionUtils.isEmpty(classTimeTables)) {
				return;
			}
			List<ClassTimeTable> results = new ArrayList<ClassTimeTable>();
			for (ClassTimeTable classTimeTable : classTimeTables) {
				initClassTimeTableStatus(classTimeTable);
				validateStatusByType(type, results, classTimeTable);
			}
			group.setClassTimeTables(results);
			relatedGroups.add(group);
		});
		return relatedGroups;
	}

	@Override
	public List<Group> getAllGroupClassTimeTables(String uuid, Integer type) {
		if (Objects.isNull(uuid)) {
			return null;
		}
		List<String> groupIds = RedisCacheManager.getCache(USER).listGetAll(uuid + SUFFIX + GROUP);
		if (CollectionUtils.isEmpty(groupIds)) {
			return null;
		}
		List<Group> groups = new ArrayList<Group>();
		groupIds.forEach(groupId -> {
			Group group = getGroupClassTimeTables(groupId, type);
			if (Objects.isNull(group))
				return;
			groups.add(group);
		});
		return groups;
	}

	/**
	 * 更新班牌终端协议类型及在线状态
	 * @param classTimeTable 班牌实体
	 */
	@Override
	public boolean updateClassTimeTableTerminal(ClassTimeTable classTimeTable) {
		String uuid = classTimeTable.getUuid();
		String terminal = classTimeTable.getTerminal();
		String status = classTimeTable.getStatus();
		if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(terminal) || StringUtils.isEmpty(status)) {
			log.error("uuid:{},terminal:{},status:{} must not null", uuid, terminal, status);
			return false;
		}
		RedisCacheManager.getCache(USER).put(uuid + SUFFIX + TERMINAL + SUFFIX + terminal, classTimeTable.getStatus());
		return true;
	}

	/**
	 * 获取班牌所属的所有群组
	 * @param uuid 班牌设备UUID
	 * @return List<String> 群组ID列表
	 */
	@Override
	public List<String> getGroups(String uuid) {
		List<String> groups = RedisCacheManager.getCache(USER).listGetAll(uuid + SUFFIX + GROUP);
		return groups;
	}

	static {
		RedisCacheManager.register(USER, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(GROUP, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(STORE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(PUSH, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(FRIEND_REQUEST, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

}
