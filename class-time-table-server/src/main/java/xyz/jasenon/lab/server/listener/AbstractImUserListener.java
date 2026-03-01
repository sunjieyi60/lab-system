package xyz.jasenon.lab.server.listener;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.listener.ImUserListener;
import xyz.jasenon.lab.core.message.MessageHelper;
import xyz.jasenon.lab.core.packets.User;
import xyz.jasenon.lab.server.config.ImServerConfig;

import java.util.Objects;

/**
 * @author WChao
 * @Desc 绑定/解绑 用户监听器抽象类
 * @date 2020-05-02 13:43
 */
public abstract class AbstractImUserListener implements ImUserListener{

    public abstract void doAfterBind(ImChannelContext imChannelContext, User user) throws ImException;

    public abstract void doAfterUnbind(ImChannelContext imChannelContext, User user) throws ImException;

    @Override
    public void onAfterBind(ImChannelContext imChannelContext, User user) throws ImException {
        ImServerConfig imServerConfig =  (ImServerConfig)imChannelContext.getImConfig();
        MessageHelper messageHelper = imServerConfig.getMessageHelper();
        //是否开启持久化
        if(isStore(imServerConfig)){
            messageHelper.getBindListener().onAfterUserBind(imChannelContext, user);
        }
        doAfterBind(imChannelContext, user);
    }

    @Override
    public void onAfterUnbind(ImChannelContext imChannelContext, User user) throws ImException {
        ImServerConfig imServerConfig =  (ImServerConfig)imChannelContext.getImConfig();
        MessageHelper messageHelper = imServerConfig.getMessageHelper();
        //是否开启持久化
        if(isStore(imServerConfig)){
            messageHelper.getBindListener().onAfterUserUnbind(imChannelContext, user);
        }
        doAfterUnbind(imChannelContext, user);
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
