/**
 * 
 */
package xyz.jasenon.lab.core;

import org.tio.core.ChannelContext;
import xyz.jasenon.lab.core.exception.ImDecodeException;

import java.nio.ByteBuffer;
/**
 * 版本: [1.0]
 * 功能说明: 
 * 作者: WChao 创建时间: 2017年7月27日 下午5:25:13
 */
public interface ImDecoder {
	
	 ImPacket decode(ByteBuffer buffer, ChannelContext channelContext) throws ImDecodeException;
}
