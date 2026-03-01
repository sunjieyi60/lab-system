package xyz.jasenon.lab.core.message;

import xyz.jasenon.lab.core.listener.ImStoreBindListener;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.core.packets.Group;

import java.util.List;

/**
 * 电子班牌消息助手接口
 * 支持班牌设备的分组筛选、加入移出分组等管理功能
 * 
 * @author WChao
 * @author Jasenon_ce (重构: 将User概念替换为ClassTimeTable班牌实体)
 * @date 2018年4月9日 下午4:31:51
 */
public interface MessageHelper {

	/**
	 * 获取IM开启持久化时绑定/解绑群组、班牌监听器
	 * @return ImStoreBindListener 监听器
	 */
	ImStoreBindListener getBindListener();

	/**
	 * 判断班牌是否在线
	 * @param uuid 班牌设备UUID
	 * @return true:在线, false:离线
	 */
	boolean isOnline(String uuid);

	/**
	 * 获取指定群组所有班牌成员信息
	 * @param groupId 群组ID
	 * @param type 筛选类型(0:所有在线班牌, 1:所有离线班牌, 2:所有班牌[在线+离线])
	 * @return Group 群组对象，包含班牌列表
	 */
	Group getGroupClassTimeTables(String groupId, Integer type);

	/**
	 * 获取班牌所属的所有群组成员信息
	 * @param uuid 班牌设备UUID
	 * @param type 筛选类型(0:所有在线班牌, 1:所有离线班牌, 2:所有班牌[在线+离线])
	 * @return List<Group> 群组列表
	 */
	List<Group> getAllGroupClassTimeTables(String uuid, Integer type);

	/**
	 * 获取关联班牌分组所有成员信息
	 * @param uuid 班牌设备UUID
	 * @param relatedGroupId 关联班牌分组ID
	 * @param type 筛选类型(0:所有在线班牌, 1:所有离线班牌, 2:所有班牌[在线+离线])
	 * @return Group 群组对象
	 */
	Group getRelatedClassTimeTables(String uuid, String relatedGroupId, Integer type);

	/**
	 * 获取所有关联班牌分组成员信息
	 * @param uuid 班牌设备UUID
	 * @param type 筛选类型(0:所有在线班牌, 1:所有离线班牌, 2:所有班牌[在线+离线])
	 * @return List<Group> 群组列表
	 */
	List<Group> getAllRelatedClassTimeTables(String uuid, Integer type);

	/**
	 * 根据在线类型获取班牌信息
	 * @param uuid 班牌设备UUID
	 * @param type 筛选类型(0:所有在线班牌, 1:所有离线班牌, 2:所有班牌[在线+离线])
	 * @return ClassTimeTable 班牌实体
	 */
	ClassTimeTable getClassTimeTableByType(String uuid, Integer type);

	/**
	 * 添加班牌到群组（班牌加入分组）
	 * @param uuid 班牌设备UUID
	 * @param groupId 群组ID
	 */
	void addGroupClassTimeTable(String uuid, String groupId);

	/**
	 * 获取群组所有班牌UUID列表
	 * @param groupId 群组ID
	 * @return List<String> 班牌UUID列表
	 */
	List<String> getGroupClassTimeTableIds(String groupId);

	/**
	 * 获取班牌所属的所有群组ID
	 * @param uuid 班牌设备UUID
	 * @return List<String> 群组ID列表
	 */
	List<String> getGroups(String uuid);

	/**
	 * 从群组中移除班牌（班牌移出分组）
	 * @param uuid 班牌设备UUID
	 * @param groupId 群组ID
	 */
	void removeGroupClassTimeTable(String uuid, String groupId);

	/**
	 * 更新班牌终端协议类型及在线状态
	 * @param classTimeTable 班牌实体信息
	 * @return true:更新成功, false:更新失败
	 */
	boolean updateClassTimeTableTerminal(ClassTimeTable classTimeTable);

}
