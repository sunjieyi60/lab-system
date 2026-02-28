package xyz.jasenon.lab.core.listener;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.packets.ClassTimeTable;

/**
 * @ClassName ImClassTimeTableListener
 * @Description 绑定/解绑用户监听器
 * @Author WChao
 * @Date 2020/1/12 14:24
 * @Version 1.0
 **/
public interface ImClassTimeTableListener {
    /**
     * 绑定用户后回调该方法
     * @param imChannelContext IM通道上下文
     * @param classTimeTable 绑定用户信息
     * @throws Exception
     * @author WChao
     */
    void onAfterBind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException;

    /**
     * 解绑用户后回调该方法
     * @param imChannelContext IM通道上下文
     * @param classTimeTable 解绑用户信息
     * @throws Exception
     * @author WChao
     */
    void onAfterUnbind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException;
}
