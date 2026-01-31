package xyz.jasenon.lab.class_time_table.t_io.protocol;

import lombok.Data;
import org.tio.core.intf.Packet;

/**
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
public class SmartBoardPacket extends Packet {

    public final static Integer MAGIC_NUMBER = 0X5A5A5A5A;
    /** 协议头长度：magic(4)+version(1)+cmdType(1)+seqId(2)+qos(1)+flags(1)+reserved(1)+checkSum(1)+length(4)=16 */
    public final static Integer HEADER_LENGTH = 16;

    private Integer magic = MAGIC_NUMBER;
    private Byte version = 0x01;
    private Byte cmdType;
    private Short seqId;
    private Byte qos;
    private Byte flags;
    private Byte reserved;
    private Byte checkSum;
    private Integer length;
    // --- End of Header ---

    // --- Start of Payload ---
    private byte[] payload;
    // --- End of Payload ---


}
