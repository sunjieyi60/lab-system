package xyz.jasenon.lab.server.listener;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.listener.ImClassTimeTableListener;
import xyz.jasenon.lab.core.message.MessageHelper;
import xyz.jasenon.lab.core.packets.ClassTimeTable;
import xyz.jasenon.lab.server.config.ImServerConfig;

import java.util.Objects;

/**
 * @author WChao
 * @Desc 绑定/解绑 用户监听器抽象类
 * @date 2020-05-02 13:43kjhgfdcxz
 */
public abstract class AbstractImUserListener implements ImClassTimeTableListener {

    public abstract void doAfterBind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException;

    public abstract void doAfterUnbind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException;

    @Override
    public void onAfterBind(ImChannelContext imChannelContext,  ClassTimeTable classTimeTable) throws ImException {
        ImServerConfig imServerConfig =  (ImServerConfig)imChannelContext.getImConfig();
        MessageHelper messageHelper = imServerConfig.getMessageHelper();
        //是否开启持久化
        if(isStore(imServerConfig)){
            messageHelper.getBindListener().onAfterClassTimeTableBind(imChannelContext, classTimeTable);
        }
        doAfterBind(imChannelContext, classTimeTable);
    }

    @Override
    public void onAfterUnbind(ImChannelContext imChannelContext, ClassTimeTable classTimeTable) throws ImException {
        ImServerConfig imServerConfig =  (ImServerConfig)imChannelContext.getImConfig();
        MessageHelper messageHelper = imServerConfig.getMessageHelper();
        //是否开启持久化
        if(isStore(imServerConfig)){
            messageHelper.getBindListener().onAfterClassTimeTableUnbind(imChannelContext, classTimeTable);
        }
        doAfterUnbind(imChannelContext, classTimeTable);
    }

    /**
     * 是否开启持久化;
     * @return
     */
    public boolean isStore(ImServerConfig imServerConfig){
        MessageHelper messageHelper = imServerConfig.getMessageHelper();
        if(imServerConfig.ON.equals(imServerConfig.getIsStore()) && Objects.nonNull(messageHelper)){
            return true;
        }
        return false;
    }

}
