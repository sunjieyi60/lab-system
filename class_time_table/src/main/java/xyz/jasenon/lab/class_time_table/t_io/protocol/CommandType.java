package xyz.jasenon.lab.class_time_table.t_io.protocol;

/**
 * @author Jasenon_ce
 * @date 2026/1/31
 */
public record CommandType(Byte cmdType) {

    public static final Byte QOS_ACK = 0x00;
    // 注册
    public static final Byte REGISTER = 0x01;
    public static final Byte REGISTER_ACK = 0x02;

    // 心跳
    public static final Byte HEARTBEAT = 0x10;
    public static final Byte HEARTBEAT_ACK = 0x11;

    // 人脸：send = Server 发出，send_ack = Client 回复（Web 下发到班牌本地特征库，一次请求内得知插入结果）
    public static final Byte FACE_SEND = 0x20;
    public static final Byte FACE_SEND_ACK = 0x21;
    public static final Byte FEATURE_UPLOAD = 0x22;

    // 门禁
    public static final Byte DOOR_OPEN = 0x30;
    public static final Byte DOOR_STATUS = 0x31;

    // 课表
    public static final Byte TIMETABLE_REQ = 0x40;
    public static final Byte TIMETABLE_RESP = 0x41;

    // OTA
    public static final Byte OTA_NOTIFY = 0x50;
    public static final Byte OTA_DOWNLOAD = 0x51;
    public static final Byte OTA_CHUNK = 0x52;
    public static final Byte OTA_PROGRESS = 0x53;
}