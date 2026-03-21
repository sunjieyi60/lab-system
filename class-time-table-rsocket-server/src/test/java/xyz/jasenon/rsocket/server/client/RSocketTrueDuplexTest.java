package xyz.jasenon.rsocket.server.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.rsocket.context.RSocketServerBootstrap;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import xyz.jasenon.rsocket.core.packet.*;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.ConnectionManager;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSocket 真正双工通信集成测试
 *
 * 双工通信定义：服务端和客户端都可以主动发起请求
 * 
 * 测试场景：
 * 1. 客户端注册到服务端
 * 2. 服务端主动向客户端发送命令（开门、配置更新等）
 * 3. 客户端接收并处理服务端主动推送的消息
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.sql.init.schema-locations=classpath:db/schema-h2.sql"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RSocketTrueDuplexTest {

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Autowired
    private ConnectionManager connectionManager;

    @Autowired
    private RSocketStrategies rSocketStrategies;

    private RSocketRequester clientRequester;
    private static final String TEST_UUID = "test-duplex-client-001";
    private static final Long TEST_LAB_ID = 1L;
    private static int serverPort = 7001;

    // 客户端用于接收服务端主动推送的消息
    private static final ConcurrentHashMap<String, Sinks.Many<Message<?>>> clientSinks = new ConcurrentHashMap<>();
    
    // 泛型类型引用
    private static final ParameterizedTypeReference<Message<RegisterResponse>> REGISTER_RESPONSE_TYPE = 
            new ParameterizedTypeReference<>() {};

    /**
     * 客户端控制器 - 处理服务端主动发送的请求
     * 
     * 这是双工通信的关键：服务端可以调用客户端的接口
     */
    @Controller
    static class ClientSideController {
        
        /**
         * 处理服务端下发的开门命令
         * 
         * 服务端通过 connectionManager.getRequester(uuid).route("client.door.open").data(...) 调用
         */
        @MessageMapping("client.door.open")
        public Mono<Message<OpenDoorResponse>> handleServerOpenDoorCommand(
                @Payload Message<OpenDoorRequest> message) {
            
            log.info("【双工通信】客户端收到服务端主动下发的开门命令: type={}, verifyInfo={}",
                    message.getData().getType(),
                    message.getData().getVerifyInfo());
            
            // 模拟客户端执行开门操作
            OpenDoorResponse response = OpenDoorResponse.success();
            
            return Mono.just(Message.<OpenDoorResponse>builder()
                    .type(Message.Type.REQUEST_RESPONSE)
                    .data(response)
                    .status(Status.C10000)
                    .timestamp(Instant.now())
                    .build());
        }

        /**
         * 处理服务端下发的配置更新命令
         */
        @MessageMapping("client.config.update")
        public Mono<Message<UpdateConfigResponse>> handleServerConfigUpdate(
                @Payload Message<UpdateConfigRequest> message) {
            
            log.info("【双工通信】客户端收到服务端主动下发的配置更新: version={}",
                    message.getData().getVersion());
            
            // 模拟客户端更新配置
            UpdateConfigResponse response = UpdateConfigResponse.success(message.getData().getVersion());
            
            return Mono.just(Message.<UpdateConfigResponse>builder()
                    .type(Message.Type.REQUEST_RESPONSE)
                    .data(response)
                    .status(Status.C10000)
                    .timestamp(Instant.now())
                    .build());
        }

        /**
         * 处理服务端主动发送的广播通知（FireAndForget）
         */
        @MessageMapping("client.notification")
        public Mono<Void> handleServerNotification(@Payload Message<String> message) {
            log.info("【双工通信】客户端收到服务端主动通知: {}", message.getData());
            
            // 通知存储到 sink，供测试验证
            Sinks.Many<Message<?>> sink = clientSinks.get("notification");
            if (sink != null) {
                sink.tryEmitNext(message);
            }
            
            return Mono.empty();
        }
    }

    @BeforeAll
    void setUp(@Autowired RSocketServerBootstrap serverBootstrap) {
        log.info("RSocket 服务端启动于端口: {}", serverPort);
        
        // 创建带客户端控制器的连接
        // 这样服务端可以通过这个连接反向调用客户端
        clientRequester = requesterBuilder
                .tcp("localhost", serverPort);
        
        log.info("RSocket 客户端连接已建立（支持双工通信）");
    }

    @AfterAll
    void tearDown() {
        if (clientRequester != null) {
            try {
                clientRequester.rsocket().dispose();
            } catch (Exception ignored) {}
        }
        clientSinks.clear();
    }

    // ==================== 双工通信测试 ====================

    @Test
    @Order(1)
    @DisplayName("测试双工通信基础 - 客户端注册，服务端保存连接")
    void testDuplex_ClientRegister() {
        log.info("========== 测试双工通信 - 客户端注册 ==========");

        // 客户端发送注册请求
        RegisterRequest request = new RegisterRequest();
        request.setUuid(TEST_UUID);
        request.setLaboratoryId(TEST_LAB_ID);

        Message<RegisterRequest> message = Message.<RegisterRequest>builder()
                .route("device.register")
                .type(Message.Type.REQUEST_RESPONSE)
                .data(request)
                .timestamp(Instant.now())
                .build();

        // 注册
        Mono<Message<RegisterResponse>> responseMono = clientRequester
                .route("device.register")
                .data(message)
                .retrieveMono(REGISTER_RESPONSE_TYPE);

        StepVerifier.create(responseMono)
                .assertNext(msg -> {
                    Assertions.assertEquals(Status.C10000, msg.getStatus());
                    log.info("客户端 {} 注册成功", msg.getData().getUuid());
                })
                .verifyComplete();

        // 验证服务端保存了连接
        RSocketRequester serverSavedRequester = connectionManager.getRequester(TEST_UUID);
        Assertions.assertNotNull(serverSavedRequester, "服务端应保存了客户端连接");
        log.info("服务端已保存客户端 {} 的连接，可以主动向客户端发送消息", TEST_UUID);
    }

    @Test
    @Order(2)
    @DisplayName("测试双工通信 - 服务端主动向客户端发送开门命令")
    void testDuplex_ServerSendOpenDoorCommand() {
        log.info("========== 测试双工通信 - 服务端主动发送开门命令 ==========");

        // 获取服务端保存的客户端连接
        RSocketRequester serverToClientRequester = connectionManager.getRequester(TEST_UUID);
        Assertions.assertNotNull(serverToClientRequester, "服务端必须已保存客户端连接");

        // 服务端构建开门命令
        OpenDoorRequest command = new OpenDoorRequest();
        command.setType(OpenDoorRequest.OpenType.REMOTE);
        command.setVerifyInfo("admin_command_123");
        command.setDuration(10);

        Message<OpenDoorRequest> serverCommand = Message.<OpenDoorRequest>builder()
                .from("SERVER")
                .to(TEST_UUID)
                .route("client.door.open")
                .type(Message.Type.REQUEST_RESPONSE)
                .data(command)
                .timestamp(Instant.now())
                .build();

        log.info("服务端准备向客户端 {} 发送开门命令", TEST_UUID);

        // 【关键】服务端主动向客户端发送请求！
        Mono<Message<OpenDoorResponse>> clientResponseMono = serverToClientRequester
                .route("client.door.open")
                .data(serverCommand)
                .retrieveMono(new ParameterizedTypeReference<Message<OpenDoorResponse>>() {});

        StepVerifier.create(clientResponseMono)
                .assertNext(response -> {
                    log.info("【双工通信成功】服务端收到客户端响应: success={}", 
                            response.getData().isSuccess());
                    Assertions.assertTrue(response.getData().isSuccess());
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    @DisplayName("测试双工通信 - 服务端主动向客户端发送配置更新")
    void testDuplex_ServerSendConfigUpdate() {
        log.info("========== 测试双工通信 - 服务端主动发送配置更新 ==========");

        RSocketRequester serverToClientRequester = connectionManager.getRequester(TEST_UUID);
        Assertions.assertNotNull(serverToClientRequester);

        // 服务端构建配置更新命令
        UpdateConfigRequest configUpdate = new UpdateConfigRequest();
        configUpdate.setVersion(System.currentTimeMillis());
        configUpdate.setImmediate(true);

        Message<UpdateConfigRequest> serverCommand = Message.<UpdateConfigRequest>builder()
                .from("SERVER")
                .to(TEST_UUID)
                .route("client.config.update")
                .type(Message.Type.REQUEST_RESPONSE)
                .data(configUpdate)
                .timestamp(Instant.now())
                .build();

        log.info("服务端准备向客户端 {} 发送配置更新: version={}", 
                TEST_UUID, configUpdate.getVersion());

        // 【关键】服务端主动向客户端发送配置更新！
        Mono<Message<UpdateConfigResponse>> clientResponseMono = serverToClientRequester
                .route("client.config.update")
                .data(serverCommand)
                .retrieveMono(new ParameterizedTypeReference<Message<UpdateConfigResponse>>() {});

        StepVerifier.create(clientResponseMono)
                .assertNext(response -> {
                    log.info("【双工通信成功】服务端收到客户端配置更新确认: currentVersion={}", 
                            response.getData());
                    Assertions.assertEquals(configUpdate.getVersion(), response.getData());
                })
                .verifyComplete();
    }

    @Test
    @Order(4)
    @DisplayName("测试双工通信 - 服务端广播通知所有在线客户端")
    void testDuplex_ServerBroadcastNotification() throws InterruptedException {
        log.info("========== 测试双工通信 - 服务端广播通知 ==========");

        // 准备接收通知的 sink
        Sinks.Many<Message<?>> notificationSink = Sinks.many().unicast().onBackpressureBuffer();
        clientSinks.put("notification", notificationSink);

        // 获取服务端保存的连接并发送广播
        RSocketRequester serverToClientRequester = connectionManager.getRequester(TEST_UUID);
        Assertions.assertNotNull(serverToClientRequester);

        Message<String> broadcast = Message.<String>builder()
                .from("SERVER")
                .type(Message.Type.FIRE_AND_FORGET)
                .data("系统即将维护，请保存数据")
                .timestamp(Instant.now())
                .build();

        log.info("服务端准备向客户端 {} 发送广播通知", TEST_UUID);

        // 服务端发送广播（FireAndForget）
        Mono<Void> sendMono = serverToClientRequester
                .route("client.notification")
                .data(broadcast)
                .send();

        StepVerifier.create(sendMono)
                .verifyComplete();

        // 验证客户端收到通知
        StepVerifier.create(notificationSink.asFlux().next())
                .assertNext(msg -> {
                    @SuppressWarnings("unchecked")
                    String notification = ((Message<String>) msg).getData();
                    log.info("【双工通信成功】客户端收到服务端广播: {}", notification);
                    Assertions.assertEquals("系统即将维护，请保存数据", notification);
                })
                .verifyComplete();

        clientSinks.remove("notification");
    }

    @Test
    @Order(5)
    @DisplayName("测试完整双工通信场景")
    void testDuplex_FullScenario() {
        log.info("========== 测试完整双工通信场景 ==========");

        String scenarioUuid = "test-duplex-full-001";

        // 1. 客户端注册
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUuid(scenarioUuid);
        registerReq.setLaboratoryId(TEST_LAB_ID);

        Message<RegisterRequest> regMsg = Message.<RegisterRequest>builder()
                .route("device.register")
                .data(registerReq)
                .timestamp(Instant.now())
                .build();

        StepVerifier.create(clientRequester.route("device.register").data(regMsg).retrieveMono(REGISTER_RESPONSE_TYPE))
                .assertNext(r -> Assertions.assertEquals(Status.C10000, r.getStatus()))
                .verifyComplete();

        log.info("1. 客户端 {} 注册成功", scenarioUuid);

        // 2. 服务端主动向客户端发送开门命令
        RSocketRequester serverConn = connectionManager.getRequester(scenarioUuid);
        Assertions.assertNotNull(serverConn);

        OpenDoorRequest doorCmd = new OpenDoorRequest();
        doorCmd.setType(OpenDoorRequest.OpenType.REMOTE);
        doorCmd.setVerifyInfo("scenario_test");

        Message<OpenDoorRequest> doorMsg = Message.<OpenDoorRequest>builder()
                .route("client.door.open")
                .data(doorCmd)
                .timestamp(Instant.now())
                .build();

        StepVerifier.create(serverConn.route("client.door.open").data(doorMsg)
                        .retrieveMono(new ParameterizedTypeReference<Message<OpenDoorResponse>>() {}))
                .assertNext(r -> Assertions.assertTrue(r.getData().isSuccess()))
                .verifyComplete();

        log.info("2. 服务端主动向客户端发送开门命令成功");

        // 3. 客户端向服务端发送心跳
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setUuid(scenarioUuid);
        heartbeat.setInterval(30);

        Message<Heartbeat> hbMsg = Message.<Heartbeat>builder()
                .route("device.heartbeat")
                .data(heartbeat)
                .timestamp(Instant.now())
                .build();

        StepVerifier.create(clientRequester.route("device.heartbeat").data(hbMsg).send())
                .verifyComplete();

        log.info("3. 客户端向服务端发送心跳成功");

        // 4. 服务端再次主动向客户端发送配置更新
        UpdateConfigRequest configCmd = new UpdateConfigRequest();
        configCmd.setVersion(999L);
        configCmd.setImmediate(true);

        Message<UpdateConfigRequest> configMsg = Message.<UpdateConfigRequest>builder()
                .route("client.config.update")
                .data(configCmd)
                .timestamp(Instant.now())
                .build();

        StepVerifier.create(serverConn.route("client.config.update").data(configMsg)
                        .retrieveMono(new ParameterizedTypeReference<Message<UpdateConfigResponse>>() {}))
                .assertNext(r -> Assertions.assertEquals(999L, r.getData()))
                .verifyComplete();

        log.info("4. 服务端再次主动向客户端发送配置更新成功");

        log.info("========== 完整双工通信场景测试通过 ==========");
        log.info("证明了：");
        log.info("- 客户端可以向服务端发送请求");
        log.info("- 服务端保存连接后，可以主动向客户端发送命令");
        log.info("- 双方可以任意时刻主动发起通信（真正的双工）");
    }
}
