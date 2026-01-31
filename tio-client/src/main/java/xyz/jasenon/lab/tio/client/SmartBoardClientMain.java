package xyz.jasenon.lab.tio.client;

import com.alibaba.fastjson2.JSON;
import org.tio.client.ClientChannelContext;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.core.Node;
import org.tio.core.Tio;
import xyz.jasenon.lab.tio.client.protocol.CommandType;
import xyz.jasenon.lab.tio.client.protocol.SmartBoardPacket;

import java.util.concurrent.TimeUnit;

/**
 * t-io 客户端入口：连接 class_time_table 服务端，发送 REGISTER / HEARTBEAT 等用于验证解码。
 * 默认连 localhost:9000，可通过 -Dhost=xxx -Dport=xxx 或环境变量覆盖。
 */
public class SmartBoardClientMain {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9000;

    public static void main(String[] args) throws Exception {
        String host = System.getProperty("host", System.getenv().getOrDefault("TIO_CLIENT_HOST", DEFAULT_HOST));
        int port = Integer.parseInt(System.getProperty("port", System.getenv().getOrDefault("TIO_CLIENT_PORT", String.valueOf(DEFAULT_PORT))));

        SmartBoardClientHandler handler = new SmartBoardClientHandler();
        SmartBoardClientListener listener = new SmartBoardClientListener();
        TioClientConfig config = new TioClientConfig(handler, listener);
        config.setHeartbeatTimeout(10_000);

        TioClient client = new TioClient(config);
        Node serverNode = new Node(host, port);
        ClientChannelContext ctx = client.connect(serverNode);

        System.out.println("已连接 " + host + ":" + port + "，发送 REGISTER 与心跳…");

        short seq = 0;
        // 发送注册
        send(ctx, CommandType.REGISTER, seq++, "client-test".getBytes());
        TimeUnit.SECONDS.sleep(2);
        while (true) {
            send(ctx, CommandType.HEARTBEAT, seq++, null);
            System.out.println(JSON.toJSONString(ctx.stat));
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private static void send(ClientChannelContext ctx, byte cmdType, short seqId, byte[] payload) throws Exception {
        SmartBoardPacket packet = new SmartBoardPacket();
        packet.setMagic(SmartBoardPacket.MAGIC_NUMBER);
        packet.setVersion((byte) 0x01);
        packet.setCmdType(cmdType);
        packet.setSeqId(seqId);
        packet.setQos((byte) 0);
        packet.setFlags((byte) 0);
        packet.setReserved((byte) 0);
        packet.setCheckSum((byte) 0);
        packet.setPayload(payload == null ? new byte[0] : payload);
        Tio.send(ctx, packet);
        String cmdName = cmdName(cmdType);
        System.out.println("[发送] " + cmdName + " seqId=" + seqId + " payloadLen=" + (payload == null ? 0 : payload.length));
    }

    private static String cmdName(byte cmd) {
        return switch (cmd) {
            case CommandType.REGISTER -> "REGISTER";
            case CommandType.REGISTER_ACK -> "REGISTER_ACK";
            case CommandType.HEARTBEAT -> "HEARTBEAT";
            case CommandType.HEARTBEAT_ACK -> "HEARTBEAT_ACK";
            case CommandType.TIMETABLE_REQ -> "TIMETABLE_REQ";
            case CommandType.TIMETABLE_RESP -> "TIMETABLE_RESP";
            default -> "0x" + Integer.toHexString(cmd & 0xff);
        };
    }
}
