package xyz.jasenon.lab.tio.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.ClientChannelContext;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.core.Node;
import org.tio.core.Tio;
import xyz.jasenon.lab.tio.client.SmartBoardClientHandler;
import xyz.jasenon.lab.tio.client.SmartBoardClientListener;
import xyz.jasenon.lab.tio.client.protocol.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智能班牌客户端GUI调试界面
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
public class SmartBoardClientGUI extends Application {
    
    private TioClient client;
    private ClientChannelContext channelContext;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    
    private TextField hostField;
    private TextField portField;
    private Button connectBtn;
    private Button disconnectBtn;
    private TextArea logArea;
    private ComboBox<String> cmdTypeCombo;
    private ComboBox<QosLevel> qosCombo;
    private TextArea payloadArea;
    private Button sendBtn;
    private Label statusLabel;
    
    private final PacketBuilder packetBuilder = new PacketBuilder();
    private final SmartBoardClientHandler handler = new SmartBoardClientHandler();
    private Thread thread;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("智能班牌客户端调试工具");
        primaryStage.setWidth(900);
        primaryStage.setHeight(700);
        
        // 创建主布局
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));
        
        // 连接区域
        HBox connectionBox = createConnectionBox();
        
        // 状态区域
        HBox statusBox = createStatusBox();
        
        // 消息发送区域
        VBox sendBox = createSendBox();
        
        // 日志区域
        VBox logBox = createLogBox();
        
        mainLayout.getChildren().addAll(connectionBox, statusBox, sendBox, logBox);
        
        Scene scene = new Scene(mainLayout);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 设置关闭事件
        primaryStage.setOnCloseRequest(e -> {
            disconnect();
            Platform.exit();
        });
        
        // 初始化日志处理器
        initLogHandler();

        thread = new Thread(()->{
            while(true){
                try{
                    Tio.send(channelContext, packetBuilder.build(CommandType.HEARTBEAT,null));
                    Thread.sleep(500L);
                }catch (Exception e){
                    log.error("心跳发送失败", e);
                }
            }
        });
        thread.start();
    }
    
    private HBox createConnectionBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label hostLabel = new Label("服务器地址:");
        hostField = new TextField("localhost");
        hostField.setPrefWidth(150);
        
        Label portLabel = new Label("端口:");
        portField = new TextField("9000");
        portField.setPrefWidth(80);
        
        connectBtn = new Button("连接");
        connectBtn.setOnAction(e -> connect());
        
        disconnectBtn = new Button("断开");
        disconnectBtn.setDisable(true);
        disconnectBtn.setOnAction(e -> disconnect());
        
        box.getChildren().addAll(hostLabel, hostField, portLabel, portField, connectBtn, disconnectBtn);
        return box;
    }
    
    private HBox createStatusBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("状态: 未连接");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
        
        box.getChildren().add(statusLabel);
        return box;
    }
    
    private VBox createSendBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: gray; -fx-border-radius: 5;");
        
        Label titleLabel = new Label("发送消息");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        HBox cmdBox = new HBox(10);
        Label cmdLabel = new Label("指令类型:");
        cmdTypeCombo = new ComboBox<>();
        cmdTypeCombo.getItems().addAll(
                "REGISTER (0x01)", "REGISTER_ACK (0x02)",
                "HEARTBEAT (0x10)", "HEARTBEAT_ACK (0x11)",
                "FACE_ENROLL (0x20)", "FACE_ENROLL_ACK (0x21)", "FEATURE_UPLOAD (0x22)",
                "DOOR_OPEN (0x30)", "DOOR_STATUS (0x31)",
                "TIMETABLE_REQ (0x40)", "TIMETABLE_RESP (0x41)",
                "OTA_NOTIFY (0x50)", "OTA_DOWNLOAD (0x51)", "OTA_CHUNK (0x52)", "OTA_PROGRESS (0x53)"
        );
        cmdTypeCombo.setValue("HEARTBEAT (0x10)");
        cmdTypeCombo.setPrefWidth(200);
        
        Label qosLabel = new Label("QoS级别:");
        qosCombo = new ComboBox<>();
        qosCombo.getItems().addAll(QosLevel.values());
        qosCombo.setValue(QosLevel.AT_MOST_ONCE);
        qosCombo.setPrefWidth(150);
        
        cmdBox.getChildren().addAll(cmdLabel, cmdTypeCombo, qosLabel, qosCombo);
        
        Label payloadLabel = new Label("Payload (文本或十六进制，如: 48656C6C6F):");
        payloadArea = new TextArea();
        payloadArea.setPrefRowCount(3);
        payloadArea.setPromptText("输入文本或十六进制数据");
        
        sendBtn = new Button("发送");
        sendBtn.setPrefWidth(100);
        sendBtn.setDisable(true);
        sendBtn.setOnAction(e -> sendMessage());
        
        box.getChildren().addAll(titleLabel, cmdBox, payloadLabel, payloadArea, sendBtn);
        return box;
    }
    
    private VBox createLogBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: gray; -fx-border-radius: 5;");
        
        HBox headerBox = new HBox(10);
        Label titleLabel = new Label("消息日志");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Button clearBtn = new Button("清空");
        clearBtn.setOnAction(e -> logArea.clear());
        headerBox.getChildren().addAll(titleLabel, clearBtn);
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(15);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        
        VBox.setVgrow(logArea, Priority.ALWAYS);
        box.getChildren().addAll(headerBox, logArea);
        return box;
    }
    
    private void connect() {
        try {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            
            SmartBoardClientListener listener = new SmartBoardClientListener() {
                @Override
                public void onAfterConnected(org.tio.core.ChannelContext channelContext, boolean b, boolean b1) throws Exception {
                    Platform.runLater(() -> {
                        connected.set(true);
                        updateConnectionStatus(true);
                        appendLog("已连接到服务器: " + host + ":" + port);
                    });
                }
                
                @Override
                public void onBeforeClose(org.tio.core.ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {
                    Platform.runLater(() -> {
                        connected.set(false);
                        updateConnectionStatus(false);
                        appendLog("连接已断开: " + (remark != null ? remark : ""));
                    });
                }
            };
            
            TioClientConfig config = new TioClientConfig(handler, listener);
            config.setHeartbeatTimeout(10000);
            
            client = new TioClient(config);
            Node serverNode = new Node(host, port);
            channelContext = client.connect(serverNode);
            
            appendLog("正在连接服务器: " + host + ":" + port);
        } catch (Exception e) {
            appendLog("连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void disconnect() {
        if (client != null && channelContext != null) {
            try {
                Tio.close(channelContext, "用户断开连接");
                connected.set(false);
                updateConnectionStatus(false);
                appendLog("已断开连接");
            } catch (Exception e) {
                appendLog("断开连接失败: " + e.getMessage());
            }
        }
    }
    
    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            statusLabel.setText("状态: 已连接");
            statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            connectBtn.setDisable(true);
            disconnectBtn.setDisable(false);
            sendBtn.setDisable(false);
        } else {
            statusLabel.setText("状态: 未连接");
            statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
            connectBtn.setDisable(false);
            disconnectBtn.setDisable(true);
            sendBtn.setDisable(true);
        }
    }
    
    private void sendMessage() {
        if (!connected.get() || channelContext == null) {
            appendLog("错误: 未连接到服务器");
            return;
        }
        
        try {
            // 解析指令类型
            String cmdStr = cmdTypeCombo.getValue();
            byte cmdType = parseCommandType(cmdStr);
            
            // 解析Payload
            String payloadStr = payloadArea.getText().trim();
            byte[] payload = parsePayload(payloadStr);
            
            // 获取QoS级别
            QosLevel qosLevel = qosCombo.getValue();
            
            // 构建数据包
            SmartBoardPacket packet = packetBuilder.build(cmdType, payload, qosLevel);
            
            // 发送
            Tio.send(channelContext, packet);
            
            appendLog(String.format("[发送] %s, seqId=%d, qos=%s, payloadLen=%d, checkSum=0x%02X",
                    cmdStr, packet.getSeqId(), qosLevel, payload.length, packet.getCheckSum()));
        } catch (Exception e) {
            appendLog("发送失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private byte parseCommandType(String cmdStr) {
        if (cmdStr.contains("REGISTER") && cmdStr.contains("0x01")) return CommandType.REGISTER;
        if (cmdStr.contains("REGISTER_ACK") && cmdStr.contains("0x02")) return CommandType.REGISTER_ACK;
        if (cmdStr.contains("HEARTBEAT") && cmdStr.contains("0x10")) return CommandType.HEARTBEAT;
        if (cmdStr.contains("HEARTBEAT_ACK") && cmdStr.contains("0x11")) return CommandType.HEARTBEAT_ACK;
        if (cmdStr.contains("FACE_ENROLL") && cmdStr.contains("0x20")) return CommandType.FACE_ENROLL;
        if (cmdStr.contains("FACE_ENROLL_ACK") && cmdStr.contains("0x21")) return CommandType.FACE_ENROLL_ACK;
        if (cmdStr.contains("FEATURE_UPLOAD") && cmdStr.contains("0x22")) return CommandType.FEATURE_UPLOAD;
        if (cmdStr.contains("DOOR_OPEN") && cmdStr.contains("0x30")) return CommandType.DOOR_OPEN;
        if (cmdStr.contains("DOOR_STATUS") && cmdStr.contains("0x31")) return CommandType.DOOR_STATUS;
        if (cmdStr.contains("TIMETABLE_REQ") && cmdStr.contains("0x40")) return CommandType.TIMETABLE_REQ;
        if (cmdStr.contains("TIMETABLE_RESP") && cmdStr.contains("0x41")) return CommandType.TIMETABLE_RESP;
        if (cmdStr.contains("OTA_NOTIFY") && cmdStr.contains("0x50")) return CommandType.OTA_NOTIFY;
        if (cmdStr.contains("OTA_DOWNLOAD") && cmdStr.contains("0x51")) return CommandType.OTA_DOWNLOAD;
        if (cmdStr.contains("OTA_CHUNK") && cmdStr.contains("0x52")) return CommandType.OTA_CHUNK;
        if (cmdStr.contains("OTA_PROGRESS") && cmdStr.contains("0x53")) return CommandType.OTA_PROGRESS;
        throw new IllegalArgumentException("未知的指令类型: " + cmdStr);
    }
    
    private byte[] parsePayload(String payloadStr) {
        if (payloadStr.isEmpty()) {
            return new byte[0];
        }
        
        // 尝试解析为十六进制
        if (payloadStr.matches("^[0-9A-Fa-f\\s]+$")) {
            String hex = payloadStr.replaceAll("\\s+", "");
            if (hex.length() % 2 != 0) {
                throw new IllegalArgumentException("十六进制长度必须是偶数");
            }
            byte[] result = new byte[hex.length() / 2];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return result;
        }
        
        // 否则作为文本处理
        return payloadStr.getBytes(StandardCharsets.UTF_8);
    }
    
    private void initLogHandler() {
        // Handler会在收到消息时自动打印到控制台
        // 如果需要更详细的GUI集成，可以修改Handler添加回调接口
    }
    
    /**
     * 添加接收到的消息到日志（供外部调用）
     */
    public void addReceivedMessage(SmartBoardPacket packet) {
        String cmdName = getCommandName(packet.getCmdType());
        String logMsg = String.format("[收到] %s, seqId=%d, qos=%s, flags=0x%02X, payloadLen=%d, checkSum=0x%02X",
                cmdName, packet.getSeqId(), packet.getQosLevel(),
                packet.getFlags() != null ? packet.getFlags() : 0,
                packet.getPayload() != null ? packet.getPayload().length : 0,
                packet.getCheckSum() != null ? packet.getCheckSum() : 0);
        appendLog(logMsg);
        
        // 如果payload不为空，显示内容预览
        if (packet.getPayload() != null && packet.getPayload().length > 0) {
            String payloadPreview = new String(packet.getPayload(), StandardCharsets.UTF_8);
            if (payloadPreview.length() > 100) {
                payloadPreview = payloadPreview.substring(0, 100) + "...";
            }
            appendLog("  Payload预览: " + payloadPreview);
        }
    }
    
    private String getCommandName(byte cmd) {
        return switch (cmd) {
            case CommandType.REGISTER -> "REGISTER (0x01)";
            case CommandType.REGISTER_ACK -> "REGISTER_ACK (0x02)";
            case CommandType.HEARTBEAT -> "HEARTBEAT (0x10)";
            case CommandType.HEARTBEAT_ACK -> "HEARTBEAT_ACK (0x11)";
            case CommandType.FACE_ENROLL -> "FACE_ENROLL (0x20)";
            case CommandType.FACE_ENROLL_ACK -> "FACE_ENROLL_ACK (0x21)";
            case CommandType.FEATURE_UPLOAD -> "FEATURE_UPLOAD (0x22)";
            case CommandType.DOOR_OPEN -> "DOOR_OPEN (0x30)";
            case CommandType.DOOR_STATUS -> "DOOR_STATUS (0x31)";
            case CommandType.TIMETABLE_REQ -> "TIMETABLE_REQ (0x40)";
            case CommandType.TIMETABLE_RESP -> "TIMETABLE_RESP (0x41)";
            case CommandType.OTA_NOTIFY -> "OTA_NOTIFY (0x50)";
            case CommandType.OTA_DOWNLOAD -> "OTA_DOWNLOAD (0x51)";
            case CommandType.OTA_CHUNK -> "OTA_CHUNK (0x52)";
            case CommandType.OTA_PROGRESS -> "OTA_PROGRESS (0x53)";
            default -> "UNKNOWN (0x" + Integer.toHexString(cmd & 0xff) + ")";
        };
    }
    
    private void appendLog(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            logArea.appendText("[" + timestamp + "] " + message + "\n");
            // 自动滚动到底部
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

