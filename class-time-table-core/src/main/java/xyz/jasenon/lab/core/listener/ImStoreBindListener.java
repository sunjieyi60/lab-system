package xyz.jasenon.lab.core.listener;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.core.packets.Group;

/**
 * IM持久化绑定班牌设备及群组监听器
 * 
 * @author WChao
 * @author Jasenon_ce (重构: 将User概念替换为ClassTimeTable班牌实体)
 * @date 2018年4月8日 下午4:09:14
 */
public interface ImStoreBindListener {

	/**
	 * 绑定群组后持久化回调该方法
	 * @param imChannelContext 通道上下文
	 * @param group 绑定群组信息
	 * @throws ImException 异常
	 */
	void onAfterGroupBind(ImChannelContext imChannelContext, Group group) throws ImException;

	/**
	 * 解绑群组后持久化回调该方法
	 * @param imChannelContext 通道上下文
	 * @param group 解绑群组信息
	 * @throws ImException 异常
	 */
	void onAfterGroupUnbind(ImChannelContext imChannelContext, Group group) throws ImException;

	/**
	 * 绑定班牌后持久化回调该方法
	 * @param imChannelContext 通道上下文
	 * @param classTimeTable 绑定班牌信息
	 * @throws ImException 异常
	 */
	void onAfterClassTimeTableBind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException;

	/**
	 * 解绑班牌后回调该方法
	 * @param imChannelContext 通道上下文
	 * @param classTimeTable 解绑班牌信息
	 * @throws ImException 异常
	 */
	void onAfterClassTimeTableUnbind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException;

}
