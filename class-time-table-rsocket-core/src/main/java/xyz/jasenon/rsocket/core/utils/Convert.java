package xyz.jasenon.rsocket.core.utils;

import com.alibaba.fastjson2.JSON;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import xyz.jasenon.rsocket.core.protocol.Message;

import java.nio.ByteBuffer;

/**
 * Message 类型转换工具类
 * 
 * 提供 Message 与 Payload 之间的转换方法
 */
public final class Convert {

    private Convert() {
        // 工具类，禁止实例化
    }

    /**
     * 将 Message 转换为 Payload
     * 
     * @param message 消息对象
     * @return RSocket Payload
     */
    public static Payload castPayload(Message message){
        byte[] buf = message.toByte();
        return ByteBufPayload.create(buf);
    }

    /**
     * 将 Payload 转换为 Message
     * 
     * @param payload RSocket Payload
     * @return Message 对象（可能需要根据 command 进一步转换为具体子类）
     */
    public static Message castMessage(Payload payload){
        ByteBuffer buf = payload.getData();
        byte[] bytes = byteBufferToBytes(buf);
        return JSON.parseObject(bytes, Message.class);
    }

    /**
     * 将 ByteBuffer 转换为 byte[]（支持 Direct ByteBuffer）
     */
    private static byte[] byteBufferToBytes(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            // Heap ByteBuffer
            return buffer.array();
        } else {
            // Direct ByteBuffer (Netty)
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return bytes;
        }
    }
}
