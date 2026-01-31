package xyz.jasenon.lab.tio.client.protocol;

/**
 * 与 class_time_table 服务端 CommandType 一致。
 */
public final class CommandType {
    public static final byte REGISTER = 0x01;
    public static final byte REGISTER_ACK = 0x02;
    public static final byte HEARTBEAT = 0x10;
    public static final byte HEARTBEAT_ACK = 0x11;
    public static final byte FACE_ENROLL = 0x20;
    public static final byte FACE_ENROLL_ACK = 0x21;
    public static final byte FEATURE_UPLOAD = 0x22;
    public static final byte DOOR_OPEN = 0x30;
    public static final byte DOOR_STATUS = 0x31;
    public static final byte TIMETABLE_REQ = 0x40;
    public static final byte TIMETABLE_RESP = 0x41;
    public static final byte OTA_NOTIFY = 0x50;
    public static final byte OTA_DOWNLOAD = 0x51;
    public static final byte OTA_CHUNK = 0x52;
    public static final byte OTA_PROGRESS = 0x53;

    private CommandType() {}
}
