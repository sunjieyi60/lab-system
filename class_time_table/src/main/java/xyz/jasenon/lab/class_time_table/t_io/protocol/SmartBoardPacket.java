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
    public final static Integer HEADER_LENGTH = 20;

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
