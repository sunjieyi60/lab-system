package xyz.jasenon.lab.server.helper.redis;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import xyz.jasenon.lab.core.cache.redis.JedisTemplate;
import xyz.jasenon.lab.core.cache.redis.RedisCache;
import xyz.jasenon.lab.core.cache.redis.RedisCacheManager;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.listener.ImStoreBindListener;
import xyz.jasenon.lab.core.message.AbstractMessageHelper;
import xyz.jasenon.lab.core.packets.*;
import xyz.jasenon.lab.core.utils.JsonKit;
import xyz.jasenon.lab.server.config.ImServerConfig;
import xyz.jasenon.lab.server.util.ChatKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Redis获取持久化+同步消息助手;
 * @author WChao
 * @date 2018年4月9日 下午4:39:30
 */
public class RedisMessageHelper extends AbstractMessageHelper{

	private Logger log = LoggerFactory.getLogger(RedisMessageHelper.class);

	private static final String SUFFIX = ":";


	public RedisMessageHelper(){
		this.imConfig =  ImConfig.Global.get();
	}
	
	@Override
	public ImStoreBindListener getBindListener() {
		
		return new RedisImStoreBindListener(imConfig, this);
	}
	
	@Override
	public boolean isOnline(String userId) {
		try{
			String keyPattern = USER+SUFFIX+userId+SUFFIX+TERMINAL;
			Set<String> terminalKeys = JedisTemplate.me().keys(keyPattern);
			if(CollectionUtils.isEmpty(terminalKeys)){
				return false;
			}
			Iterator<String> terminalKeyIterator = terminalKeys.iterator();
			while(terminalKeyIterator.hasNext()){
				String terminalKey = terminalKeyIterator.next();
				terminalKey = terminalKey.substring(terminalKey.indexOf(userId));
				String isOnline = RedisCacheManager.getCache(USER).get(terminalKey, String.class);
				if(UserStatusType.ONLINE.getStatus().equals(isOnline)){
					return true;
				}
			}
		}catch(Exception e){
			log.error(e.toString(),e);
		}
		return false;
	}
	
	@Override
	public List<String> getGroupUsers(String groupId) {
		String groupUserKey = groupId+SUFFIX+USER;
		return RedisCacheManager.getCache(GROUP).listGetAll(groupUserKey);
	}

	
	@Override
	public void writeMessage(String timelineTable, String timelineId, ChatBody chatBody) {
		double score = chatBody.getCreateTime();
		RedisCacheManager.getCache(timelineTable).sortSetPush(timelineId, score, chatBody);
	}


	@Override
	public void addGroupUser(String userId, String groupId) {
		List<String> users = RedisCacheManager.getCache(GROUP).listGetAll(groupId);
		if(users.contains(userId)){
			return;
		}
		RedisCacheManager.getCache(GROUP).listPushTail(groupId, userId);
	}

	@Override
	public void removeGroupUser(String userId, String groupId) {
		RedisCacheManager.getCache(GROUP).listRemove(groupId, userId);
	}

	@Override
	public UserMessageData getFriendsOfflineMessage(String userId, String fromUserId) {
		String userFriendKey = USER+SUFFIX+userId+SUFFIX+fromUserId;
		List<String> messageList = RedisCacheManager.getCache(PUSH).sortSetGetAll(userFriendKey);
		List<ChatBody> messageDataList = JsonKit.toArray(messageList, ChatBody.class);
		RedisCacheManager.getCache(PUSH).remove(userFriendKey);
		return putFriendsMessage(new UserMessageData(userId), messageDataList, null);
	}

	@Override
	public UserMessageData getFriendsOfflineMessage(String userId) {
		UserMessageData messageData = new UserMessageData(userId);
		try{
			Set<String> userKeys = JedisTemplate.me().keys(PUSH+SUFFIX+USER+SUFFIX+userId);
			//获取好友离线消息;
			if(CollectionUtils.isNotEmpty(userKeys)){
				List<ChatBody> messageList = new ArrayList<ChatBody>();
				Iterator<String> userKeyIterator = userKeys.iterator();
				while(userKeyIterator.hasNext()){
					String userKey = userKeyIterator.next();
					userKey = userKey.substring(userKey.indexOf(USER+SUFFIX));
					// BUG修复：使用 PUSH cache 而不是 GROUP cache
					// 因为消息是通过 writeMessage(PUSH, ...) 写入的
					List<String> messages = RedisCacheManager.getCache(PUSH).sortSetGetAll(userKey);
					RedisCacheManager.getCache(PUSH).remove(userKey);
					messageList.addAll(JsonKit.toArray(messages, ChatBody.class));
				}
				putFriendsMessage(messageData, messageList, null);
			}
			List<String> groupIdList = RedisCacheManager.getCache(USER).listGetAll(userId+SUFFIX+GROUP);
			//获取群组离线消息;
			if(CollectionUtils.isNotEmpty(groupIdList)){
				groupIdList.forEach(groupId->{
					UserMessageData groupMessageData = getGroupOfflineMessage(userId, groupId);
					if(Objects.isNull(groupMessageData))return;
					putGroupMessage(messageData, groupMessageData.getGroups().get(groupId));
				});
			}
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
		return messageData;
	}

	@Override
	public UserMessageData getGroupOfflineMessage(String userId, String groupId) {
		UserMessageData messageData = new UserMessageData(userId);
		String userGroupKey = GROUP+SUFFIX+groupId+SUFFIX+userId;
		List<String> messages = RedisCacheManager.getCache(PUSH).sortSetGetAll(userGroupKey);
		if(CollectionUtils.isEmpty(messages)) {
			return messageData;
		}
		putGroupMessage(messageData, JsonKit.toArray(messages, ChatBody.class));
		RedisCacheManager.getCache(PUSH).remove(userGroupKey);
		return messageData;
	}

	@Override
	public UserMessageData getFriendHistoryMessage(String userId, String fromUserId, Double beginTime, Double endTime, Integer offset, Integer count) {
		String sessionId = ChatKit.sessionId(userId, fromUserId);
		String userSessionKey = USER+SUFFIX+sessionId;
		List<String> messages = getHistoryMessage(userSessionKey, beginTime, endTime, offset, count);
		UserMessageData messageData = new UserMessageData(userId);
		putFriendsMessage(messageData, JsonKit.toArray(messages, ChatBody.class), fromUserId);
		return messageData;
	}

	@Override
	public UserMessageData getGroupHistoryMessage(String userId, String groupId, Double beginTime, Double endTime, Integer offset, Integer count) {
		String groupKey = GROUP+SUFFIX+groupId;
		List<String> messages = getHistoryMessage(groupKey, beginTime, endTime, offset, count);
		UserMessageData messageData = new UserMessageData(userId);
		putGroupMessage(messageData, JsonKit.toArray(messages, ChatBody.class));
		return messageData;
	}

	private List<String> getHistoryMessage(String historyKey, Double beginTime, Double endTime, Integer offset, Integer count){
		boolean isTimeBetween = (beginTime != null && endTime != null);
		boolean isPage = (offset != null && count != null);
		RedisCache storeCache = RedisCacheManager.getCache(STORE);
		//消息区间，不分页
		if(isTimeBetween && !isPage){
			return storeCache.sortSetGetAll(historyKey, beginTime, endTime);
			//消息区间，并且分页;
		}else if(isTimeBetween && isPage){
			return storeCache.sortSetGetAll(historyKey, beginTime, endTime, offset, count);
			//所有消息，并且分页;
		}else if(isPage){
			return storeCache.sortSetGetAll(historyKey, 0, Double.MAX_VALUE, offset, count);
			//所有消息，不分页;
		}else{
			return storeCache.sortSetGetAll(historyKey);
		}
	}

	/**
	 * 放入用户群组消息;
	 * @param userMessage
	 * @param messages
	 */
	public UserMessageData putGroupMessage(UserMessageData userMessage, List<ChatBody> messages){
		if(Objects.isNull(userMessage)|| CollectionUtils.isEmpty(messages)) {
			return userMessage;
		}
		messages.forEach(chatBody -> {
			String groupId = chatBody.getGroupId();
			if(StringUtils.isEmpty(groupId)) {
				return;
			}
			List<ChatBody> groupMessages = userMessage.getGroups().get(groupId);
			if(CollectionUtils.isEmpty(groupMessages)){
				groupMessages = new ArrayList();
				userMessage.getGroups().put(groupId, groupMessages);
			}
			groupMessages.add(chatBody);
		});
		return userMessage;
	}

	/**
	 * 组装放入用户好友消息;
	 * @param userMessage
	 * @param messages
	 */
	public UserMessageData putFriendsMessage(UserMessageData userMessage , List<ChatBody> messages, String friendId){
		if(Objects.isNull(userMessage)|| CollectionUtils.isEmpty(messages)) {
			return userMessage;
		}
		messages.forEach(chatBody -> {
			String fromId = chatBody.getFrom();
			if(StringUtils.isEmpty(fromId)) {
				return;
			}
			String targetFriendId = friendId;
			if(StringUtils.isEmpty(targetFriendId)){
				targetFriendId = fromId;
			}
			List<ChatBody> friendMessages = userMessage.getFriends().get(targetFriendId);
			if(CollectionUtils.isEmpty(friendMessages)){
				friendMessages = new ArrayList();
				userMessage.getFriends().put(targetFriendId, friendMessages);
			}
			friendMessages.add(chatBody);
		});
		return userMessage;
	}

	/**
	 * 获取群组所有成员信息
	 * @param groupId 群组ID
	 * @param type(0:所有在线用户,1:所有离线用户,2:所有用户[在线+离线])
	 * @return
	 */
	@Override
	public Group getGroupUsers(String groupId, Integer type) {
		if(Objects.isNull(groupId) || Objects.isNull(type)) {
			log.warn("group:{} or type:{} is null", groupId, type);
			return null;
		}
		Group group = RedisCacheManager.getCache(GROUP).get(groupId+SUFFIX+INFO , Group.class);
		if(Objects.isNull(group)) {
			return null;
		}
		List<String> userIds = this.getGroupUsers(groupId);
		if(CollectionUtils.isEmpty(userIds)) {
			return null;
		}
		List<User> users = new ArrayList<User>();
		userIds.forEach(userId -> {
			User user = getClassTimeTableByType(userId, type);
			if(Objects.isNull(user))return;
			validateStatusByType(type, users, user);
		});
		group.setUsers(users);
		return group;
	}

	/**
	 * 根据获取type校验是否组装User
	 * @param type
	 * @param users
	 * @param user
	 */
	private void validateStatusByType(Integer type, List<User> users, User user) {
		String status = user.getStatus();
		if(UserStatusType.ONLINE.getNumber() == type && UserStatusType.ONLINE.getStatus().equals(status)){
			users.add(user);
		}else if(UserStatusType.OFFLINE.getNumber() == type && UserStatusType.OFFLINE.getStatus().equals(status)){
			users.add(user);
		}else if(UserStatusType.ALL.getNumber() == type){
			users.add(user);
		}
	}

	@Override
	public User getClassTimeTableByType(String uuid, Integer type){
		User user = RedisCacheManager.getCache(USER).get(uuid +SUFFIX+INFO, User.class);
		if(Objects.isNull(user)) {
			return null;
		}
		boolean isOnline = this.isOnline(uuid);
		String status = isOnline ? UserStatusType.ONLINE.getStatus() : UserStatusType.OFFLINE.getStatus();
		if( UserStatusType.ONLINE.getNumber() == type && isOnline){
			user.setStatus(status);
			return user;
		}else if(UserStatusType.OFFLINE.getNumber() == type && !isOnline){
			user.setStatus(status);
			return user;
		}else if(type == UserStatusType.ALL.getNumber()){
			user.setStatus(status);
			return user;
		}
		return null;
	}

	@Override
	public Group getFriendUsers(String userId , String friendGroupId, Integer type) {
		boolean isTrue = Objects.isNull(userId) || Objects.isNull(friendGroupId) || Objects.isNull(type);
		if(isTrue) {
			log.warn("userId:{} or friendGroupId:{} or type:{} is null");
			return null;
		}
		List<Group> friends = RedisCacheManager.getCache(USER).get(userId+SUFFIX+FRIENDS, List.class);
		if(CollectionUtils.isEmpty(friends)) {
			return null;
		}
		for(Group group : friends){
			if(!friendGroupId.equals(group.getGroupId()))continue;
			List<User> users = group.getUsers();
			if(CollectionUtils.isEmpty(users)) {
				return group;
			}
			List<User> userResults = new ArrayList<User>();
			for(User user : users){
				initUserStatus(user);
				validateStatusByType(type, userResults, user);
			}
			group.setUsers(userResults);
			return group;
		}
		return null;
	}

	/**
	 * 初始化用户在线状态;
	 * @param user
	 */
	public boolean initUserStatus(User user){
		if(Objects.isNull(user) || Objects.isNull(user.getUserId())) {
			return false;
		}
		String userId = user.getUserId();
		boolean isOnline = this.isOnline(userId);
		if(isOnline){
			user.setStatus(UserStatusType.ONLINE.getStatus());
		}else{
			user.setStatus(UserStatusType.OFFLINE.getStatus());
		}
		return true;
	}

	/**
	 * 获取好友分组所有成员信息
	 * @param userId 用户ID
	 * @param type(0:所有在线用户,1:所有离线用户,2:所有用户[在线+离线])
	 * @return
	 */
	@Override
	public List<Group> getAllFriendUsers(String userId, Integer type) {
		if(Objects.isNull(userId)) {
			return null;
		}
		List<JSONObject> friendJsonArray = RedisCacheManager.getCache(USER).get(userId+SUFFIX+FRIENDS, List.class);
		if(CollectionUtils.isEmpty(friendJsonArray)) {
			return null;
		}
		List<Group> friends = new ArrayList<Group>();
		friendJsonArray.forEach(groupJson -> {
			Group group = JSONObject.toJavaObject(groupJson, Group.class);
			List<User> users = group.getUsers();
			if(CollectionUtils.isEmpty(users)) {
				return;
			}
			List<User> userResults = new ArrayList<User>();
			for(User user : users){
				initUserStatus(user);
				validateStatusByType(type, userResults, user);
			}
			group.setUsers(userResults);
			friends.add(group);
		});
		return friends;
	}

	@Override
	public List<Group> getAllGroupUsers(String userId, Integer type) {
		if(Objects.isNull(userId)) {
			return null;
		}
		List<String> groupIds = RedisCacheManager.getCache(USER).listGetAll(userId+SUFFIX+GROUP);
		if(CollectionUtils.isEmpty(groupIds)) {
			return null;
		}
		List<Group> groups = new ArrayList<Group>();
		groupIds.forEach(groupId -> {
			Group group = getGroupUsers(groupId, type);
			if(Objects.isNull(group))return;
			groups.add(group);
		});
		return groups;
	}

	/**
	 * 更新用户终端协议类型及在线状态;
	 * @param user 用户信息
	 */
	@Override
	public boolean updateUserTerminal(User user){
		String userId = user.getUserId();String terminal = user.getTerminal();String status = user.getStatus();
		if(StringUtils.isEmpty(userId) || StringUtils.isEmpty(terminal) || StringUtils.isEmpty(status)) {
			log.error("userId:{},terminal:{},status:{} must not null", userId, terminal, status);
			return false;
		}
		RedisCacheManager.getCache(USER).put(userId+SUFFIX+TERMINAL+SUFFIX+terminal, user.getStatus());
		return true;
	}

	/**
	 * @param userId        发送好友申请的用户id
	 * @param toUserId      处理好友申请的用户id
	 * @param friendReqBody 请求实体
	 */
	@Override
	public void addFriendRequest(String userId, String toUserId, FriendReqBody friendReqBody) {
		// 确保状态为待处理（如未设置）
		if (friendReqBody.getResult() == null) {
			friendReqBody.setResult(FriendReqBody.Result.PENDING);
		}
		double score = friendReqBody.getCreateTime();
		ImServerConfig imServerConfig = ImConfig.Global.get();
		boolean isStore = ImServerConfig.ON.equals(imServerConfig.getIsStore());
		if (!ChatKit.isOnline(toUserId,isStore)){
			RedisCacheManager.getCache(PUSH).sortSetPush(FRIEND_REQUEST+SUFFIX+toUserId, score, friendReqBody);
		}
		RedisCacheManager.getCache(FRIEND_REQUEST).put(friendReqBody.getRequestId(), friendReqBody);
		RedisCacheManager.getCache(FRIEND_REQUEST).sortSetPush(userId, score, friendReqBody.getRequestId());
		RedisCacheManager.getCache(FRIEND_REQUEST).sortSetPush(toUserId, score, friendReqBody.getRequestId());
	}

	/**
	 * 获取用户添加好友请求
	 * @param userId    查询用户的id
	 * @param requestId 添加好友请求的唯一 id
	 * @return 添加好友请求的详细实体
	 */
	@Override
	public FriendReqBody getFriendRequest(String userId, String requestId) {
		List<String> requestIds = RedisCacheManager.getCache(FRIEND_REQUEST).sortSetGetAll(userId);
		if (!CollectionUtils.isEmpty(requestIds) && requestIds.contains(requestId)) {
			return RedisCacheManager.getCache(FRIEND_REQUEST).get(requestId, FriendReqBody.class);
		}
		return null;
	}

	/**
	 * 更新好友申请请求的状态
	 * @param friendReqBody 需要更新的实体
	 */
	@Override
	public void updateFriendRequest(FriendReqBody friendReqBody) {
		RedisCacheManager.getCache(FRIEND_REQUEST).put(friendReqBody.getRequestId(), friendReqBody);
	}

	/**
	 * 建立好友关系
	 *
	 * @param userId       处理人id
	 * @param friendUserId 请求人id
	 */
	@Override
	public void addFriendRelation(String userId, String friendUserId) {
		long score = System.currentTimeMillis();
		RedisCacheManager.getCache(USER).sortSetPush(userId + SUFFIX + FRIENDS, score, friendUserId);
		RedisCacheManager.getCache(USER).sortSetPush(friendUserId + SUFFIX + FRIENDS, score, userId);
	}

	/**
	 * 获取所有好友的id
	 *
	 * @param userId 请求人的id
	 * @return 请求人的好友id列表
	 */
	@Override
	public List<String> getFriendIdsListByUserId(String userId) {
		List<String> friendIds = RedisCacheManager.getCache(USER).sortSetGetAll(userId + SUFFIX + FRIENDS);
		return friendIds;
	}

	/**
	 * 获取接收到的或发送的好友申请
	 *
	 * @param userId 用户id
	 * @return 好友申请列表
	 */
	@Override
	public List<FriendReqBody> getFriendReqBodyListByUserId(String userId) {
		List<String> requestIds = RedisCacheManager.getCache(FRIEND_REQUEST).sortSetGetAll(userId);
		Iterator<String> iterator = requestIds.iterator();
		List<FriendReqBody> result = new ArrayList<>();
		while(iterator.hasNext()){
			String requestId = iterator.next();
			FriendReqBody reqBody = RedisCacheManager.getCache(FRIEND_REQUEST).get(requestId, FriendReqBody.class);
			result.add(reqBody);
		}
		return result;
	}

	/**
	 * 检查双方是否是好友
	 *
	 * @param userId       操作人id
	 * @param friendUserId 好友id
	 * @return 检查结果
	 */
	@Override
	public boolean isFriend(String userId, String friendUserId) {
		boolean aIsBFriends = RedisCacheManager.getCache(USER).sortSetGetAll(userId + SUFFIX + FRIENDS).contains(friendUserId);
		boolean bIsAFriends = RedisCacheManager.getCache(USER).sortSetGetAll(friendUserId + SUFFIX + FRIENDS).contains(userId);
		return aIsBFriends && bIsAFriends;
	}

	/**
	 * 获取用户拥有的群组;
	 * @param userId
	 * @return
	 */
	@Override
	public List<String> getGroups(String userId) {
		List<String> groups = RedisCacheManager.getCache(USER).listGetAll(userId+SUFFIX+GROUP);
		return groups;
	}

	static{
		RedisCacheManager.register(USER, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(GROUP, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(STORE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(PUSH, Integer.MAX_VALUE, Integer.MAX_VALUE);
		RedisCacheManager.register(FRIEND_REQUEST, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

}
