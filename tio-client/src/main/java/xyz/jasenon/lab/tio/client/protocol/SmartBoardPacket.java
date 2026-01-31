package xyz.jasenon.lab.tio.client.protocol;

import lombok.Data;
import org.tio.core.intf.Packet;

/**
 * 与 class_time_table 服务端协议一致，用于测试解码。
 */
@Data
public class SmartBoardPacket extends Packet {

    public static final int MAGIC_NUMBER = 0x5A5A5A5A;
    /** 协议头长度：magic(4)+version(1)+cmdType(1)+seqId(2)+qos(1)+flags(1)+reserved(1)+checkSum(1)+length(4)=16 */
    public static final int HEADER_LENGTH = 16;

    private Integer magic = MAGIC_NUMBER;
    private Byte version = 0x01;
    private Byte cmdType;
    private Short seqId;
    private Byte qos;
    private Byte flags;
    private Byte reserved;
    private Byte checkSum;
    private Integer length;

    private byte[] payload;
}
