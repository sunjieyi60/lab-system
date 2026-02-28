/**
 * 
 */
package xyz.jasenon.lab.core.protocol;

import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.packets.Command;

/**
 * 转换不同协议消息包;
 * @author WChao
 *
 */
public interface IProtocolConverter {
	/**
	 * 转化请求包
	 * @param body
	 * @param command
	 * @param imChannelContext
	 * @return
	 */
	ImPacket ReqPacket(byte[] body, Command command, ImChannelContext imChannelContext);
	/**
	 * 转化响应包
	 * @param body
	 * @param command
	 * @param imChannelContext
	 * @return
	 */
	ImPacket RespPacket(byte[] body, Command command, ImChannelContext imChannelContext);

	/**
	 * 转化响应包
	 * @param imPacket
	 * @param command
	 * @param imChannelContext
	 * @return
	 */
	ImPacket RespPacket(ImPacket imPacket, Command command, ImChannelContext imChannelContext);
}
