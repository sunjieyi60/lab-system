/**
 * 
 */
package xyz.jasenon.lab.server.command.handler;

import org.apache.commons.lang3.StringUtils;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.ImStatus;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.packets.*;
import xyz.jasenon.lab.core.utils.JsonKit;
import xyz.jasenon.lab.server.command.AbstractCmdHandler;
import xyz.jasenon.lab.server.command.handler.userInfo.IClassTimeTableInfo;
import xyz.jasenon.lab.server.command.handler.userInfo.PersistentClassTimeTableInfo;
import xyz.jasenon.lab.server.config.ImServerConfig;
import xyz.jasenon.lab.server.protocol.ProtocolManager;

import java.util.Objects;

/**
 * 版本: [1.0]
 * 功能说明: 获取设备信息消息命令
 * @author : WChao 创建时间: 2017年9月18日 下午4:08:47
 */
public class ClassTimeTableReqHandler extends AbstractCmdHandler {

	/**
	 * 持久化设备信息接口
	 */
	private IClassTimeTableInfo persistentUserInfo;

	public ClassTimeTableReqHandler(){
		persistentUserInfo = new PersistentClassTimeTableInfo();
	}
	@Override
	public Command command() {
		return Command.COMMAND_GET_CLASS_TIME_TABLE_REQ;
	}

	@Override
	public ImPacket handler(ImPacket packet, ImChannelContext imChannelContext) throws ImException {
//		UserReqBody userReqBody = JsonKit.toBean(packet.getBody(),UserReqBody.class);
		ClassTimeTableReqBody classTimeTableReqBody = JsonKit.toBean(packet.getBody(), ClassTimeTableReqBody.class);
//		String userId = userReqBody.getUserId();
		if (classTimeTableReqBody == null) {
			return ProtocolManager.Converter.respPacket(ClassTimeTableRespBody.failed(ImStatus.C10004, "设备未注册"), imChannelContext);
		}
		String uuid = classTimeTableReqBody.getUuid();
		if(StringUtils.isEmpty(uuid)) {
			return ProtocolManager.Converter.respPacket(ClassTimeTableRespBody.failed(ImStatus.C10004, "设备uuid为空"), imChannelContext);
		}
		//(0:所有在线设备,1:所有离线设备,2:所有设备[在线+离线]);
		Integer type = classTimeTableReqBody.getType() == null ? ClassTimeTableStatusType.ALL.getNumber() : classTimeTableReqBody.getType();
		if(Objects.isNull(ClassTimeTableStatusType.valueOf(type))){
			return ProtocolManager.Converter.respPacket(ClassTimeTableRespBody.failed(ImStatus.C10004, "未知的type"), imChannelContext);
		}
		RespBody respBody = new RespBody(Command.COMMAND_GET_CLASS_TIME_TABLE_RESP);
		ImServerConfig imServerConfig = ImConfig.Global.get();
		//是否开启持久化;
		boolean isStore = ImServerConfig.ON.equals(imServerConfig.getIsStore());
		if(isStore){
			respBody.setData(persistentUserInfo.getClassTimeTableInfo(classTimeTableReqBody, imChannelContext));
		}
		//在线用户
		if(Objects.equals(ClassTimeTableStatusType.ONLINE.getNumber(), classTimeTableReqBody.getType())){
			respBody.setCode(ImStatus.C10005.getCode()).setMsg(ImStatus.C10005.getMsg());
			//离线用户;
		}else if(Objects.equals(ClassTimeTableStatusType.OFFLINE.getNumber(), classTimeTableReqBody.getType())){
			respBody.setCode(ImStatus.C10006.getCode()).setMsg(ImStatus.C10006.getMsg());
			//在线+离线用户;
		}else if(Objects.equals(ClassTimeTableStatusType.ALL.getNumber(), classTimeTableReqBody.getType())){
			respBody.setCode(ImStatus.C10003.getCode()).setMsg(ImStatus.C10003.getMsg());
		}
		return ProtocolManager.Converter.respPacket(respBody, imChannelContext);
	}

	public IClassTimeTableInfo getPersistentUserInfo() {
		return persistentUserInfo;
	}

	public void setPersistentUserInfo(IClassTimeTableInfo persistentUserInfo) {
		this.persistentUserInfo = persistentUserInfo;
	}

}
