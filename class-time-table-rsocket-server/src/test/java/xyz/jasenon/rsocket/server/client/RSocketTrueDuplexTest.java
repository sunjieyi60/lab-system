//package xyz.jasenon.rsocket.server.client;
//
//import io.rsocket.SocketAcceptor;
//import io.rsocket.transport.netty.client.TcpClientTransport;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.messaging.rsocket.RSocketRequester;
//import org.springframework.messaging.rsocket.RSocketStrategies;
//import org.springframework.test.context.ActiveProfiles;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//import xyz.jasenon.rsocket.core.Api;
//import xyz.jasenon.rsocket.core.Const;
//import xyz.jasenon.rsocket.core.model.Config;
//import xyz.jasenon.rsocket.core.packet.*;
//import xyz.jasenon.rsocket.core.protocol.Message;
//import xyz.jasenon.rsocket.core.protocol.Command;
//import xyz.jasenon.rsocket.core.protocol.Status;
//import xyz.jasenon.rsocket.core.rsocket.AbstractConnectionManager;
//import xyz.jasenon.rsocket.core.rsocket.client.ClientResponder;
//import xyz.jasenon.rsocket.core.rsocket.client.handler.Handler;
//import xyz.jasenon.rsocket.core.rsocket.client.handler.HandlerManager;
//
//import java.time.Duration;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicReference;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
///**
// * RSocket 真实单连接双工通信测试
// *
// * 测试架构：
// * - 客户端：使用 RSocketRequester + ClientResponder（SocketAcceptor）
// * - 服务器：Spring Boot RSocket Server（@MessageMapping）
// * - 通信方式：单个 RSocket 连接实现双工通信
// *
// * 测试流程：
// * 1. 客户端创建带 ClientResponder 的 RSocketRequester（单连接）
// * 2. 客户端通过该连接发送注册请求 -> 服务器
// * 3. 服务器保存客户端连接的 Requester
// * 4. 服务器通过保存的 Requester 向客户端发送命令
// * 5. 客户端的 ClientResponder 接收命令，路由到 Handler 处理
// * 6. Handler 处理完成，返回响应给服务器
// */
//@Slf4j
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//public class RSocketTrueDuplexTest {
//
//    @Autowired
//    private RSocketStrategies rSocketStrategies;
//
//    @Autowired
//    private AbstractConnectionManager connectionManager;
//
//    @Autowired
//    private Api api;
//
//    @Value("${spring.rsocket.server.port}")
//    private int serverPort;
//
//    // 客户端使用单个连接
//    private RSocketRequester clientRequester;
//    private String testUuid;
//    private static final Long TEST_LAB_ID = 1L;
//
//    // 用于验证 Handler 是否被调用
//    private static final AtomicBoolean registerHandlerCalled = new AtomicBoolean(false);
//    private static final AtomicBoolean updateConfigHandlerCalled = new AtomicBoolean(false);
//    private static final AtomicReference<String> handlerReceivedMessage = new AtomicReference<>();
//
//    @BeforeEach
//    void setUp() {
//        // 生成唯一的测试 UUID
//        this.testUuid = "test-duplex-" + System.nanoTime();
//
//        // 清理状态
//        connectionManager.getAllConnections().clear();
//        registerHandlerCalled.set(false);
//        updateConfigHandlerCalled.set(false);
//        handlerReceivedMessage.set(null);
//        HandlerManager.getAllHandlers().clear();
//
//        // 注册测试用的 Handler
//        registerTestHandlers();
//
//        log.info("测试设置完成，Server 端口: {}, UUID: {}", serverPort, this.testUuid);
//    }
//
//    @AfterEach
//    void tearDown() {
//        // 关闭客户端连接
//        if (clientRequester != null) {
//            try {
//                clientRequester.rsocket().dispose();
//            } catch (Exception e) {
//                // 忽略
//            }
//            clientRequester = null;
//        }
//        // 清理服务器连接
//        if (testUuid != null) {
//            connectionManager.remove(testUuid);
//        }
//        // 清理 Handler
//        HandlerManager.getAllHandlers().clear();
//    }
//
//    /**
//     * 注册测试用的 Handler
//     */
//    private void registerTestHandlers() {
//        HandlerManager.register(new RegisterTestHandler());
//        HandlerManager.register(new UpdateConfigTestHandler());
//        HandlerManager.register(new HeartbeatTestHandler());
//
//        log.info("测试 Handler 已注册，数量: {}", HandlerManager.getAllHandlers().size());
//    }
//
//    // ==================== 测试用 Handler 实现（非静态内部类，可访问 testUuid）====================
//
//    /**
//     * 注册命令处理器
//     */
//    public class RegisterTestHandler implements Handler {
//        @Override
//        public Command command() {
//            return Command.REGISTER;
//        }
//
//        @Override
//        public Mono<Message> handle(Message message) {
//            log.info("【RegisterHandler】收到服务器命令: command={}, from={}",
//                    message.getCommand(), message.getFrom());
//
//            registerHandlerCalled.set(true);
//            handlerReceivedMessage.set("REGISTER:" + message.getCommand());
//
//            // 构造注册响应
//            RegisterResponse response = new RegisterResponse();
//            response.setCommand(Command.REGISTER);
//            response.setUuid(testUuid);
//            response.setStatus(Status.C10000, "设备注册处理成功");
//
//            log.info("【RegisterHandler】发送响应: uuid={}", response.getUuid());
//
//            return Mono.just(response);
//        }
//    }
//
//    /**
//     * 配置更新处理器
//     */
//    public class UpdateConfigTestHandler implements Handler {
//        @Override
//        public Command command() {
//            return Command.UPDATE_CONFIG;
//        }
//
//        @Override
//        public Mono<Message> handle(Message message) {
//            log.info("【UpdateConfigHandler】收到服务器命令: command={}, data={}",
//                    message.getCommand(), message);
//
//            updateConfigHandlerCalled.set(true);
//            handlerReceivedMessage.set("UPDATE_CONFIG:" + message.getCommand());
//
//            Long version = null;
//            if (message instanceof UpdateConfigRequest) {
//                version = ((UpdateConfigRequest) message).getVersion();
//            }
//
//            // 构造配置更新响应
//            UpdateConfigResponse response = new UpdateConfigResponse();
//            response.setCommand(Command.UPDATE_CONFIG);
//            response.setSuccess(true);
//            response.setStatus(Status.C10000, "配置更新成功，version=" + version);
//
//            log.info("【UpdateConfigHandler】发送响应: success={}", response.isSuccess());
//
//            return Mono.just(response);
//        }
//    }
//
//    /**
//     * 心跳处理器
//     */
//    public class HeartbeatTestHandler implements Handler {
//        @Override
//        public Command command() {
//            return Command.HEARTBEAT;
//        }
//
//        @Override
//        public Mono<Message> handle(Message message) {
//            log.info("【HeartbeatHandler】收到心跳检查");
//
//            Heartbeat response = new Heartbeat();
//            response.setCommand(Command.HEARTBEAT);
//            response.setUuid(testUuid);
//            response.setInterval(30);
//            response.setConfigUpdated(false);
//            response.setStatus(Status.C10000, "心跳正常");
//
//            return Mono.just(response);
//        }
//    }
//
//    // ==================== 核心方法：创建单连接双工客户端 ====================
//
//    /**
//     * 创建带 ClientResponder 的客户端连接（单连接双工）
//     *
//     * 关键：使用 RSocketRequester.builder() 配置底层的 RSocketConnector，
//     * 添加 SocketAcceptor 来处理服务器下发的命令
//     */
//    private RSocketRequester createDuplexClient() {
//        // 创建 ClientResponder 处理服务器命令
//        ClientResponder clientResponder = new ClientResponder();
//
//        // 创建带 SocketAcceptor 的 RSocketRequester
//        return RSocketRequester.builder()
//                .rsocketStrategies(rSocketStrategies)
//                .rsocketConnector(connector ->
//                    connector.acceptor(SocketAcceptor.with(clientResponder))
//                )
//                .connect(TcpClientTransport.create("localhost", serverPort))
//                .block(Duration.ofSeconds(10));
//    }
//
//    // ==================== 测试方法 ====================
//
//    /**
//     * 测试 1: 验证 Handler 注册
//     */
//    @Test
//    void testHandlerRegistration() {
//        assertThat(HandlerManager.get(Command.REGISTER)).isNotNull();
//        assertThat(HandlerManager.get(Command.UPDATE_CONFIG)).isNotNull();
//        assertThat(HandlerManager.get(Command.HEARTBEAT)).isNotNull();
//        log.info("测试1通过: Handler 注册成功");
//    }
//
//    /**
//     * 测试 2: 单连接双工通信 - 客户端发送请求，服务器响应
//     */
//    @Test
//    void testClientToServer() {
//        // 创建双工客户端
//        clientRequester = createDuplexClient();
//        log.info("【测试2】双工客户端已创建");
//
//        // 客户端发送注册请求
//        RegisterRequest request = RegisterRequest.create(testUuid, TEST_LAB_ID);
//
//        Mono<RegisterResponse> responseMono = clientRequester
//                .route(Const.Route.DEVICE_REGISTER)
//                .data(request)
//                .retrieveMono(RegisterResponse.class);
//
//        // 验证服务器响应
//        StepVerifier.create(responseMono.timeout(Duration.ofSeconds(10)))
//                .assertNext(response -> {
//                    assertThat(response.getCode()).isEqualTo(Status.C10000.getCode());
//                    assertThat(response.getUuid()).isEqualTo(testUuid);
//                    log.info("【测试2】客户端收到服务器响应: code={}, msg={}",
//                            response.getCode(), response.getMsg());
//                })
//                .verifyComplete();
//
//        log.info("测试2通过: Client -> Server 通信正常");
//    }
//
//    /**
//     * 测试 3: 单连接双工通信 - 服务器向客户端发送命令，触发 Handler
//     *
//     * 这是真正的双工测试：服务器通过保存的连接主动向客户端发送命令
//     */
//    @Test
//    void testServerToClientWithHandler() {
//        log.info("=== 开始单连接双工测试 ===");
//
//        // 步骤1: 创建双工客户端（带 ClientResponder）
//        clientRequester = createDuplexClient();
//        log.info("【步骤1】双工客户端已创建，包含 ClientResponder");
//
//        // 步骤2: 客户端发送注册请求，服务器保存连接
//        RegisterRequest registerRequest = RegisterRequest.create(testUuid, TEST_LAB_ID);
//
//        RegisterResponse registerResponse = clientRequester
//                .route(Const.Route.DEVICE_REGISTER)
//                .data(registerRequest)
//                .retrieveMono(RegisterResponse.class)
//                .block(Duration.ofSeconds(10));
//
//        assertThat(registerResponse.getCode()).isEqualTo(Status.C10000.getCode());
//        log.info("【步骤2】客户端注册成功: uuid={}", registerResponse.getUuid());
//
//        // 步骤3: 等待服务器确认设备在线
//        await().atMost(Duration.ofSeconds(5))
//                .until(() -> connectionManager.isOnline(testUuid));
//        log.info("【步骤3】服务器确认设备在线");
//
//        // 步骤4: 服务器通过保存的 Requester 向客户端发送命令
//        RSocketRequester serverRequester = connectionManager.getRequester(testUuid);
//        assertThat(serverRequester).isNotNull();
//        assertThat(serverRequester.rsocket().isDisposed()).isFalse();
//        log.info("【步骤4】服务器获取到客户端连接");
//
//        // 步骤5: 服务器发送配置更新命令给客户端
//        // Server -> Client 使用 ServerSend 接口（command）
//        UpdateConfigRequest serverCommand = new UpdateConfigRequest();
//        serverCommand.setCommand(Command.UPDATE_CONFIG);
//        serverCommand.setVersion(100L);
//        serverCommand.setImmediate(true);
//        serverCommand.setStatus(Status.C10000, "服务器要求更新配置");
//        serverCommand.setConfig(Config.Default());
//
//        log.info("【步骤5】服务器发送配置更新命令给客户端...");
//
//        // 使用 Api 发送命令（ServerSend 接口，使用 command）
//        Mono<Message> responseMono = api.sendToClient(serverCommand, serverRequester);
//        responseMono.subscribe();
//        // 等待 Handler 被调用（可能需要一些时间）
//        await().atMost(Duration.ofSeconds(5))
//                .until(() -> updateConfigHandlerCalled.get());
//
//        log.info("【步骤5】客户端 Handler 已被调用！");
//
//        // 验证 Handler 收到了正确的命令
//        assertThat(updateConfigHandlerCalled.get()).isTrue();
//        assertThat(handlerReceivedMessage.get()).contains("UPDATE_CONFIG");
//
//        // 验证响应（可选，因为 FireAndForget 不返回响应）
//        // 如果使用 requestResponse，可以验证响应内容
//
//        log.info("=== 单连接双工测试通过 ===");
//    }
//
//    /**
//     * 测试 4: 使用 Request-Response 模式的双工通信
//     *
//     * 服务器发送命令，客户端 Handler 处理并返回响应
//     */
//    @Test
//    void testServerToClientRequestResponse() {
//        log.info("=== 开始 Request-Response 双工测试 ===");
//
//        // 步骤1: 创建双工客户端
//        clientRequester = createDuplexClient();
//
//        // 步骤2: 注册设备
//        RegisterRequest registerRequest = RegisterRequest.create(testUuid, TEST_LAB_ID);
//        RegisterResponse registerResponse = clientRequester
//                .route(Const.Route.DEVICE_REGISTER)
//                .data(registerRequest)
//                .retrieveMono(RegisterResponse.class)
//                .block(Duration.ofSeconds(10));
//
//        assertThat(registerResponse.getCode()).isEqualTo(Status.C10000.getCode());
//        log.info("设备注册成功: uuid={}", registerResponse.getUuid());
//
//        // 步骤3: 等待设备在线
//        await().atMost(Duration.ofSeconds(5))
//                .until(() -> connectionManager.isOnline(testUuid));
//
//        // 步骤4: 服务器发送命令并等待响应
//        RSocketRequester serverRequester = connectionManager.getRequester(testUuid);
//
//        // 构造心跳检查命令
//        Heartbeat heartbeatCommand = new Heartbeat();
//        heartbeatCommand.setRoute(Const.Route.DEVICE_HEARTBEAT);
//        heartbeatCommand.setCommand(Command.HEARTBEAT);
//        heartbeatCommand.setUuid(testUuid);
//        heartbeatCommand.setStatus(Status.C10000, "服务器心跳检查");
//
//        log.info("服务器发送心跳检查命令...");
//
//        // 发送并等待响应
//        Mono<Message> responseMono = api.sendToClient(heartbeatCommand, serverRequester);
//
//        Message response = responseMono.block(Duration.ofSeconds(10));
//
//        // 验证响应
//        assertThat(response).isNotNull();
//        assertThat(response.getCode()).isEqualTo(Status.C10000.getCode());
//        log.info("收到客户端响应: code={}, msg={}", response.getCode(), response.getMsg());
//
//        log.info("=== Request-Response 双工测试通过 ===");
//    }
//
//    /**
//     * 测试 5: 完整双工场景
//     */
//    @Test
//    void testFullDuplexScenario() {
//        log.info("=== 开始完整双工场景测试 ===");
//
//        // 1. 创建双工客户端
//        clientRequester = createDuplexClient();
//        log.info("【1】双工客户端已创建");
//
//        // 2. 客户端 -> 服务器：注册
//        RegisterRequest registerRequest = RegisterRequest.create(testUuid, TEST_LAB_ID);
//        RegisterResponse registerResponse = clientRequester
//                .route(Const.Route.DEVICE_REGISTER)
//                .data(registerRequest)
//                .retrieveMono(RegisterResponse.class)
//                .block(Duration.ofSeconds(10));
//
//        assertThat(registerResponse.getCode()).isEqualTo(Status.C10000.getCode());
//        log.info("【2】客户端注册成功");
//
//        // 3. 等待设备在线
//        await().atMost(Duration.ofSeconds(5))
//                .until(() -> connectionManager.isOnline(testUuid));
//        log.info("【3】设备已在线");
//
//        // 4. 服务器 -> 客户端：发送配置更新命令
//        RSocketRequester serverRequester = connectionManager.getRequester(testUuid);
//
//        UpdateConfigRequest configCommand = new UpdateConfigRequest();
//        configCommand.setVersion(200L);
//        configCommand.setImmediate(true);
//        configCommand.setStatus(Status.C10000, "更新配置命令");
//
//        log.info("【4】服务器发送配置更新命令...");
//
//        // 使用 FireAndForget（单向，ServerSend 接口，使用 command）
//        Mono<Void> fireAndForgetMono = api.sendAndForgetToClient(configCommand, serverRequester);
//        fireAndForgetMono.block(Duration.ofSeconds(5));
//
//        // 等待 Handler 被调用
//        await().atMost(Duration.ofSeconds(5))
//                .until(() -> updateConfigHandlerCalled.get());
//
//        log.info("【4】客户端 Handler 已处理配置更新命令");
//
//        // 5. 验证
//        assertThat(updateConfigHandlerCalled.get()).isTrue();
//
//        log.info("=== 完整双工场景测试通过 ===");
//    }
//}
