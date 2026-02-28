/**
 * 
 */
package xyz.jasenon.lab.server.processor.friend;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.packets.FriendHandleReqBody;
import xyz.jasenon.lab.core.packets.FriendListRespBody;
import xyz.jasenon.lab.core.packets.FriendReqBody;
import xyz.jasenon.lab.core.packets.FriendReqListRespBody;
import xyz.jasenon.lab.core.packets.FriendRespBody;
import xyz.jasenon.lab.server.processor.SingleProtocolCmdProcessor;

/**
 * 好友命令处理器接口
 * 
 * 功能说明：
 * 1. 处理好友申请
 * 2. 处理好友申请响应（同意/拒绝）
 * 3. 获取好友列表
 * 
 * 参考 {@link xyz.jasenon.lab.server.processor.login.LoginCmdProcessor} 的设计
 * 
 * @author Jasenon_ce
 * @date 2026/2/26
 */
public interface FriendCmdProcessor extends SingleProtocolCmdProcessor {

    /**
     * 发送好友申请
     * 
     * @param reqBody 好友申请请求体
     * @param imChannelContext 通道上下文
     * @return 好友申请响应体
     */
    FriendRespBody sendFriendRequest(FriendReqBody reqBody, ImChannelContext imChannelContext);

    /**
     * 处理好友申请（同意/拒绝）
     * 
     * @param reqBody 处理好友申请请求体
     * @param imChannelContext 通道上下文
     * @return 处理响应体
     */
    FriendRespBody handleFriendRequest(FriendHandleReqBody reqBody, ImChannelContext imChannelContext);

    /**
     * 获取好友列表
     * 
     * @param userId 用户ID
     * @param imChannelContext 通道上下文
     * @return 好友列表响应体
     */
    FriendListRespBody getFriendList(String userId, ImChannelContext imChannelContext);

    /**
     * 获取好友申请列表
     * 
     * @param userId 用户ID
     * @param type 申请类型：1-我收到的，2-我发送的，null/0-全部
     * @param imChannelContext 通道上下文
     * @return 好友申请列表响应体
     */
    FriendReqListRespBody getFriendRequestList(String userId, Integer type, ImChannelContext imChannelContext);

    /**
     * 添加好友成功回调
     * 
     * @param userId 用户ID
     * @param friendUserId 好友用户ID
     * @param imChannelContext 通道上下文
     */
    void onAddFriendSuccess(String userId, String friendUserId, ImChannelContext imChannelContext);

    /**
     * 添加好友失败回调
     * 
     * @param userId 用户ID
     * @param friendUserId 好友用户ID
     * @param reason 失败原因
     * @param imChannelContext 通道上下文
     */
    void onAddFriendFailed(String userId, String friendUserId, String reason, ImChannelContext imChannelContext);
}
