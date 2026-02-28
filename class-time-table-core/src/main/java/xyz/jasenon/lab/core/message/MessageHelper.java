package xyz.jasenon.lab.core.message;

import xyz.jasenon.lab.core.listener.ImStoreBindListener;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.core.packets.Group;

import java.util.List;
/**
 * @author WChao
 * @date 2018年4月9日 下午4:31:51
 */
public interface MessageHelper {
	/**
	 * 获取IM开启持久化时绑定/解绑群组、用户监听器;
	 * @return
	 */
	 ImStoreBindListener getBindListener();
	/**
	 * 判断用户是否在线
	 * @param userId 用户ID
	 * @return
	 */
	 boolean isOnline(String userId);
	/**
	 * 获取指定群组所有成员信息
	 * @param groupId 群组ID
	 * @param type(0:所有在线用户,1:所有离线用户,2:所有用户[在线+离线])
	 * @return
	 */
	 Group getGroupUsers(String groupId, Integer type);
	/**
	 * 获取用户所有群组成员信息
	 * @param userId 用户ID
	 * @param type(0:所有在线用户,1:所有离线用户,2:所有用户[在线+离线])
	 * @return
	 */
	 List<Group> getAllGroupUsers(String userId, Integer type);
	/**
	 * 获取好友分组所有成员信息
	 * @param userId 用户ID
	 * @param friendGroupId 好友分组ID
	 * @param type(0:所有在线用户,1:所有离线用户,2:所有用户[在线+离线])
	 * @return
	 */
	 Group getFriendUsers(String userId, String friendGroupId, Integer type);
	/**
	 * 获取好友分组所有成员信息
	 * @param userId 用户ID
	 * @param type(0:所有在线用户,1:所有离线用户,2:所有用户[在线+离线])
	 * @return
	 */
	 List<Group> getAllFriendUsers(String userId, Integer type);
	/**
	 * 根据在线类型获取用户信息;
	 *
	 * @param type(0:所有在线用户,1:所有离线用户,2:所有用户[在线+离线])
	 * @param uuid                                  用户ID
	 * @return
	 */
	 ClassTimeTable getClassTimeTableByType(String uuid, Integer type);
	/**
	 * 添加群组成员
	 * @param userId 用户ID
	 * @param groupId 群组ID
	 */
	 void addGroupUser(String userId, String groupId);
	/**
	 * 获取群组所有成员;
	 * @param groupId 群组ID
	 * @return
	 */
	 List<String> getGroupUsers(String groupId);
	/**
	 * 获取用户拥有的群组ID;
	 * @param userId 用户ID
	 * @return
	 */
	 List<String> getGroups(String userId);

	/**
	 * 移除群组用户
	 * @param userId 用户ID
	 * @param groupId 群组ID
	 */
	 void removeGroupUser(String userId, String groupId);

	/**
	 * 更新用户终端协议类型及在线状态;(在线:online:离线:offline)
	 * @param classTimeTable 客户端信息
	 */
	 boolean updateUserTerminal(ClassTimeTable classTimeTable);


}
