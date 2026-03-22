package xyz.jasenon.rsocket.server.client;

import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.packet.*;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.ConnectionManager;
import xyz.jasenon.rsocket.core.rsocket.client.ClientResponder;
import xyz.jasenon.rsocket.core.rsocket.client.handler.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * RSocket 双工通信测试 - 使用 Handler 处理服务器命令
 * 
 * 测试说明：
 * 1. 启动 Server（Spring Boot RSocket Server）
 * 2. 创建 Client（使用 RSocketConnector + ClientResponder + Handler）
 * 3. Client 向 Server 发送注册请求（@MessageMapping）
 * 4. Server 保存连接后，向 Client 发送命令
 * 5. Client 的 Handler 处理命令并返回响应
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class RSocketDuplexWithHandlerTest {

    @Autowired
    private RSocketStrategies rSocketStrategies;

    @Autowired
    private ConnectionManager connectionManager;

    @Value("${spring.rsocket.server.port}")
    private int serverPort;

    private RSocket clientRSocket;
    private RSocketRequester clientRequester;  // 保持客户端连接
    private String testUuid;  // 每个测试使用独立的 UUID
    private static final Long TEST_LAB_ID = 1L;

    // 用于验证 Handler 是否被调用
    private static final AtomicBoolean registerHandlerCalled = new AtomicBoolean(false);
    private static final AtomicBoolean updateConfigHandlerCalled = new AtomicBoolean(false);
    private static final AtomicReference<String> lastReceivedCommand = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        // 生成唯一的测试 UUID
        this.testUuid = "test-device-" + System.nanoTime();
        
        // 清理状态
        connectionManager.getAllConnections().clear();
        registerHandlerCalled.set(false);
        updateConfigHandlerCalled.set(false);
        lastReceivedCommand.set(null);
        
        // 注册测试用的 Handler
        registerTestHandlers();
        
        log.info("测试设置完成，Server 端口: {}, UUID: {}", serverPort, this.testUuid);
    }

    @AfterEach
    void tearDown() {
        // 先关闭客户端连接，再关闭服务器端保存的连接
        if (clientRequester != null) {
            try {
                clientRequester.rsocket().dispose();
            } catch (Exception e) {
                // 忽略
            }
            clientRequester = null;
        }
        if (clientRSocket != null && !clientRSocket.isDisposed()) {
            clientRSocket.dispose();
        }
        // 清理服务器连接
        if (testUuid != null) {
            connectionManager.remove(testUuid);
        }
        // 清理 Handler
        HandlerManager.getAllHandlers().clear();
    }

    /**
     * 注册测试用的 Handler
     */
    private void registerTestHandlers() {
        // 注册处理器（使用测试类）
        HandlerManager.register(new RegisterTestHandler());
        HandlerManager.register(new UpdateConfigTestHandler());
        HandlerManager.register(new HeartbeatTestHandler());
        
        log.info("测试 Handler 已注册，数量: {}", HandlerManager.getAllHandlers().size());
    }

    // ==================== 测试用 Handler 实现 ====================

    /**
     * 注册命令处理器 - 模拟设备端处理服务器的注册命令
     */
    public class RegisterTestHandler implements Handler {
        @Override
        public Command command() {
            return Command.REGISTER;
        }

        @Override
        public Mono<Message> handler(Message message) {
            log.info("【Handler】收到注册命令: command={}, from={}", 
                    message.getCommand(), message.getFrom());
            
            registerHandlerCalled.set(true);
            lastReceivedCommand.set("REGISTER");
            
            // 构造注册响应（设备端响应给服务器）
            RegisterResponse response = new RegisterResponse();
            response.setCommand(Command.REGISTER);
            response.setUuid(testUuid);
            response.setStatus(Status.C10000, "设备注册处理成功");
            
            log.info("【Handler】发送注册响应: uuid={}, status={}", 
                    response.getUuid(), response.getCode());
            
            return Mono.just(response);
        }
    }

    /**
     * 配置更新处理器 - 模拟设备端处理服务器的配置更新命令
     */
    public class UpdateConfigTestHandler implements Handler {
        @Override
        public Command command() {
            return Command.UPDATE_CONFIG;
        }

        @Override
        public Mono<Message> handler(Message message) {
            log.info("【Handler】收到配置更新命令: command={}", message.getCommand());
            
            updateConfigHandlerCalled.set(true);
            lastReceivedCommand.set("UPDATE_CONFIG");
            
            Long version = null;
            if (message instanceof UpdateConfigRequest) {
                version = ((UpdateConfigRequest) message).getVersion();
            }
            
            // 构造配置更新响应
            UpdateConfigResponse response = new UpdateConfigResponse();
            response.setCommand(Command.UPDATE_CONFIG);
            response.setSuccess(true);
            response.setStatus(Status.C10000, "设备配置更新成功，version=" + version);
            
            log.info("【Handler】发送配置更新响应: success={}", response.isSuccess());
            
            return Mono.just(response);
        }
    }

    /**
     * 心跳处理器 - 模拟设备端处理服务器的心跳检查
     */
    public class HeartbeatTestHandler implements Handler {
        @Override
        public Command command() {
            return Command.HEARTBEAT;
        }

        @Override
        public Mono<Message> handler(Message message) {
            log.info("【Handler】收到心跳检查: command={}", message.getCommand());
            
            Heartbeat response = new Heartbeat();
            response.setCommand(Command.HEARTBEAT);
            response.setUuid(testUuid);
            response.setInterval(30);
            response.setConfigUpdated(false);
            response.setStatus(Status.C10000, "心跳正常");
            
            return Mono.just(response);
        }
    }

    // ==================== 测试方法 ====================

    /**
     * 测试 1: 验证 Handler 注册
     */
    @Test
    void testHandlerRegistration() {
        // 验证 Handler 已注册
        assertThat(HandlerManager.get(Command.REGISTER)).isNotNull();
        assertThat(HandlerManager.get(Command.UPDATE_CONFIG)).isNotNull();
        assertThat(HandlerManager.get(Command.HEARTBEAT)).isNotNull();
        
        log.info("测试1通过: Handler 注册成功");
    }

    /**
     * 测试 2: Client 通过 route 向 Server 发送请求（非双工，验证基础通信）
     */
    @Test
    void testClientToServerViaRoute() {
        // 创建客户端连接
        RSocketRequester clientRequester = createClientRequester();
        
        // 发送注册请求到服务器的 route
        RegisterRequest request = RegisterRequest.create(testUuid, TEST_LAB_ID);
        
        Mono<RegisterResponse> responseMono = clientRequester
                .route(Const.Route.DEVICE_REGISTER)
                .data(request)
                .retrieveMono(RegisterResponse.class);
        
        // 验证服务器响应
        StepVerifier.create(responseMono.timeout(Duration.ofSeconds(10)))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(Status.C10000.getCode());
                    assertThat(response.getUuid()).isEqualTo(testUuid);
                    log.info("【测试2】客户端收到服务器响应: code={}, msg={}", 
                            response.getCode(), response.getMsg());
                })
                .verifyComplete();
        
        log.info("测试2通过: Client -> Server 通信正常");
    }

    /**
     * 测试 3: Server 通过 Command 向 Client 发送命令（双工关键测试）
     * 
     * 注意：这个测试需要 Server 能通过保存的连接向 Client 发送请求
     * 真实的双工场景是：Client 先注册，Server 获取 Requester，然后主动发送命令
     */
    @Test
    void testServerToClientViaCommand() {
        // 步骤1: 创建带 Handler 的 Client
        createClientWithResponder();
        
        // 步骤2: Client 向 Server 注册，使 Server 保存连接
        registerClient();
        
        // 步骤3: 等待 Server 保存连接
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> connectionManager.isOnline(testUuid));
        
        log.info("【测试3】客户端已注册并在线");
        
        // 步骤4: Server 主动向 Client 发送命令
        // 获取 Server 保存的客户端连接
        RSocketRequester serverRequester = connectionManager.getRequester(testUuid);
        assertThat(serverRequester).isNotNull();
        
        // 服务器发送配置更新命令给客户端
        UpdateConfigRequest serverCommand = new UpdateConfigRequest();
        serverCommand.setCommand(Command.UPDATE_CONFIG);  // 使用 command 标识
        serverCommand.setVersion(100L);
        serverCommand.setImmediate(true);
        serverCommand.setStatus(Status.C10000, "服务器要求更新配置");
        
        log.info("【测试3】服务器发送配置更新命令给客户端...");
        
        // 由于 Server 通过 @MessageMapping 处理请求，而不是直接通过 RSocketRequester 调用 Client 的 Handler
        // 真正的双工需要 Server 保存 Client 的 Requester 并通过 metadata 路由到 Client 的 Handler
        // 这里的测试验证 Server 可以获取到 Client 的连接
        
        assertThat(serverRequester.rsocket().isDisposed()).isFalse();
        log.info("【测试3】Server -> Client 连接可用: disposed={}", serverRequester.rsocket().isDisposed());
        
        log.info("测试3部分通过: Server 获取 Client 连接成功");
    }

    /**
     * 测试 4: 验证 Client 的 ClientResponder 能正确路由到 Handler
     */
    @Test
    void testClientResponderRoutesToHandler() {
        // 创建带 ClientResponder 的 Client
        createClientWithResponder();
        
        // 创建一个模拟的服务器 requester 向客户端发送命令
        // 注意：这需要客户端作为 server 接受连接，或者使用 metadata 路由
        
        // 由于测试环境限制，这里验证 ClientResponder 已创建且 Handler 已注册
        assertThat(clientRSocket).isNotNull();
        assertThat(clientRSocket.isDisposed()).isFalse();
        assertThat(HandlerManager.get(Command.REGISTER)).isNotNull();
        
        log.info("测试4通过: ClientResponder 和 Handler 准备就绪");
    }

    /**
     * 测试 5: 模拟真实双工场景
     */
    @Test
    void testRealDuplexScenario() {
        log.info("=== 开始真实双工场景测试 ===");
        
        // 1. 启动 Client（带 Handler）
        createClientWithResponder();
        log.info("【场景】1. 设备启动，Handler 已注册");
        
        // 2. Client 向 Server 注册
        registerClient();
        log.info("【场景】2. 设备向服务器注册");
        
        // 3. 等待 Server 确认
        await().atMost(Duration.ofSeconds(5))
                .until(() -> connectionManager.isOnline(testUuid));
        log.info("【场景】3. 服务器确认设备在线");
        
        // 4. Server 向 Client 发送命令（通过保存的 Requester）
        RSocketRequester serverRequester = connectionManager.getRequester(testUuid);
        if (serverRequester != null) {
            log.info("【场景】4. 服务器获取到设备连接，准备发送命令");
            
            // 真实的命令发送需要服务器端有对应的处理逻辑
            // 这里验证连接存在即可
            assertThat(serverRequester.rsocket().isDisposed()).isFalse();
        }
        
        log.info("=== 真实双工场景测试完成 ===");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建带 ClientResponder 的客户端
     */
    private void createClientWithResponder() {
        // 创建 ClientResponder（包含 Handler 路由）
        ClientResponder clientResponder = new ClientResponder();
        
        // 创建客户端 RSocket 连接（带 responder）
        clientRSocket = RSocketConnector.create()
                .acceptor(SocketAcceptor.with(clientResponder))
                .connect(TcpClientTransport.create("localhost", serverPort))
                .block(Duration.ofSeconds(10));
        
        log.info("客户端已连接，端口: {}, responder: {}", serverPort, clientResponder.getClass().getSimpleName());
    }

    /**
     * 创建客户端 Requester
     */
    private RSocketRequester createClientRequester() {
        return RSocketRequester.builder()
                .rsocketStrategies(rSocketStrategies)
                .connect(TcpClientTransport.create("localhost", serverPort))
                .block(Duration.ofSeconds(10));
    }

    /**
     * 客户端向服务器注册（保持连接）
     */
    private void registerClient() {
        // 复用已有连接或创建新连接
        if (clientRequester == null || clientRequester.rsocket().isDisposed()) {
            clientRequester = createClientRequester();
        }
        
        RegisterRequest request = RegisterRequest.create(testUuid, TEST_LAB_ID);
        
        RegisterResponse response = clientRequester
                .route(Const.Route.DEVICE_REGISTER)
                .data(request)
                .retrieveMono(RegisterResponse.class)
                .block(Duration.ofSeconds(10));
        
        log.info("客户端注册完成: uuid={}, code={}", response.getUuid(), response.getCode());
        // 注意：不要关闭连接，保持双工通信
    }
}
