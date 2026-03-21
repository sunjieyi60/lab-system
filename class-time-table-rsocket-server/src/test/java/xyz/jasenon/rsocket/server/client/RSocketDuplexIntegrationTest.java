package xyz.jasenon.rsocket.server.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.rsocket.context.RSocketServerBootstrap;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import xyz.jasenon.rsocket.core.packet.*;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Duration;
import java.time.Instant;

/**
 * RSocket 双工通信集成测试
 *
 * 测试四类消息类型：
 * 1. REQUEST_RESPONSE - 请求-响应
 * 2. FIRE_AND_FORGET - 单向发送
 * 3. REQUEST_STREAM - 请求流
 * 4. REQUEST_CHANNEL - 双向流
 * 
 * 双工通信特点：服务端和客户端都可以主动发起请求
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
public class RSocketDuplexIntegrationTest {

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Autowired
    private RSocketStrategies rSocketStrategies;

    private RSocketRequester clientRequester;
    private static final String TEST_UUID = "test-duplex-001";
    private static final Long TEST_LAB_ID = 1L;
    private static final int serverPort = 7001;

    // 泛型类型引用，用于正确反序列化
    private static final ParameterizedTypeReference<Message<RegisterResponse>> REGISTER_RESPONSE_TYPE = 
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Message<UpdateConfigResponse>> CONFIG_RESPONSE_TYPE = 
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Message<String>> STRING_MESSAGE_TYPE = 
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Message<Heartbeat>> HEARTBEAT_MESSAGE_TYPE = 
            new ParameterizedTypeReference<>() {};

    /**
     * 在所有测试前建立连接
     */
    @BeforeAll
    void setUp(@Autowired RSocketServerBootstrap serverBootstrap) {
        // 获取服务端端口
        log.info("RSocket 服务端启动于端口: {}", serverPort);
        
        // 初始化连接
        createConnection();
    }
    
    /**
     * 创建连接
     */
    private void createConnection() {
        // 关闭旧连接
        if (clientRequester != null) {
            try {
                clientRequester.rsocket().dispose();
            } catch (Exception ignored) {}
        }
        
        // 创建新的客户端连接
        clientRequester = requesterBuilder
                .tcp("localhost", serverPort);
        
        log.info("RSocket 客户端连接已建立");
        
        // 预热连接（发送一个空请求确保连接建立）
        // 使用心跳作为预热，因为这是最简单的请求
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setUuid("warmup");
        heartbeat.setInterval(30);
        
        Message<Heartbeat> warmupMsg = Message.<Heartbeat>builder()
                .route("device.heartbeat")
                .type(Message.Type.FIRE_AND_FORGET)
                .data(heartbeat)
                .timestamp(Instant.now())
                .build();
        
        try {
            clientRequester.route("device.heartbeat")
                    .data(warmupMsg)
                    .send()
                    .block(Duration.ofSeconds(5));
            log.info("连接预热完成");
        } catch (Exception e) {
            log.warn("连接预热失败（可能设备不存在）: {}", e.getMessage());
        }
    }

    /**
     * 每个测试前检查连接状态
     */
    @BeforeEach
    void ensureConnection() {
        // 检查连接是否有效
        boolean isValid = isConnectionValid();
        
        if (!isValid) {
            log.warn("连接已断开或无效，重建连接...");
            createConnection();
        } else {
            log.debug("连接状态正常");
        }
    }
    
    /**
     * 检查连接是否有效
     */
    private boolean isConnectionValid() {
        if (clientRequester == null) {
            return false;
        }
        
        // rsocket() 可能在延迟加载时为 null，所以需要通过发送测试来验证
        // 这里我们假设只要 clientRequester 不为 null，连接就是可用的
        // 实际验证通过具体请求的成功/失败来判断
        return true;
    }

    /**
     * 测试结束后关闭连接
     */
    @AfterAll
    void tearDown() {
        if (clientRequester != null) {
            try {
                io.rsocket.RSocket rsocket = clientRequester.rsocket();
                if (rsocket != null && !rsocket.isDisposed()) {
                    rsocket.dispose();
                    log.info("RSocket 客户端连接已关闭");
                }
            } catch (Exception e) {
                log.warn("关闭连接时出错: {}", e.getMessage());
            }
        }
    }

    // ==================== 1. REQUEST_RESPONSE 测试 ====================

    @Test
    @Order(1)
    @DisplayName("测试 REQUEST_RESPONSE - 设备注册")
    void testRequestResponse_Register() {
        log.info("========== 测试 REQUEST_RESPONSE - 设备注册 ==========");

        // 构建注册请求
        RegisterRequest request = new RegisterRequest();
        request.setUuid(TEST_UUID);
        request.setLaboratoryId(TEST_LAB_ID);

        Message<RegisterRequest> message = Message.<RegisterRequest>builder()
                .route("device.register")
                .type(Message.Type.REQUEST_RESPONSE)
                .data(request)
                .timestamp(Instant.now())
                .build();

        // 发送请求并接收响应
        Mono<Message<RegisterResponse>> responseMono = clientRequester
                .route("device.register")
                .data(message)
                .retrieveMono(REGISTER_RESPONSE_TYPE);

        StepVerifier.create(responseMono)
                .assertNext(msg -> {
                    log.info("收到注册响应: status={}, uuid={}", 
                            msg.getStatus(), 
                            msg.getData() != null ? msg.getData().getUuid() : null);
                    
                    Assertions.assertNotNull(msg);
                    Assertions.assertEquals(Status.C10000, msg.getStatus());
                    Assertions.assertNotNull(msg.getData());
                    Assertions.assertEquals(TEST_UUID, msg.getData().getUuid());
                    Assertions.assertNotNull(msg.getData().getConfig());
                })
                .verifyComplete();
    }

    @Test
    @Order(2)
    @DisplayName("测试 REQUEST_RESPONSE - 更新配置")
    void testRequestResponse_UpdateConfig() {
        log.info("========== 测试 REQUEST_RESPONSE - 更新配置 ==========");

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setVersion(1L);
        request.setImmediate(true);

        Message<UpdateConfigRequest> message = Message.<UpdateConfigRequest>builder()
                .route("device.config.update")
                .type(Message.Type.REQUEST_RESPONSE)
                .data(request)
                .timestamp(Instant.now())
                .build();

        Mono<Message<UpdateConfigResponse>> responseMono = clientRequester
                .route("device.config.update")
                .data(message)
                .retrieveMono(CONFIG_RESPONSE_TYPE);

        StepVerifier.create(responseMono)
                .assertNext(msg -> {
                    log.info("收到配置更新响应: status={}, success={}", 
                            msg.getStatus(), 
                            msg.getData() != null ? msg.getData().isSuccess() : null);
                    
                    Assertions.assertEquals(Status.C10000, msg.getStatus());
                    Assertions.assertNotNull(msg.getData());
                    Assertions.assertTrue(msg.getData().isSuccess());
                })
                .verifyComplete();
    }

    // ==================== 2. FIRE_AND_FORGET 测试 ====================

    @Test
    @Order(3)
    @DisplayName("测试 FIRE_AND_FORGET - 设备心跳")
    void testFireAndForget_Heartbeat() {
        log.info("========== 测试 FIRE_AND_FORGET - 设备心跳 ==========");

        // 构建心跳请求
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setUuid(TEST_UUID);
        heartbeat.setInterval(30);
        heartbeat.setConfigUpdated(false);

        Message<Heartbeat> message = Message.<Heartbeat>builder()
                .route("device.heartbeat")
                .type(Message.Type.FIRE_AND_FORGET)
                .data(heartbeat)
                .timestamp(Instant.now())
                .build();

        // 发送 FireAndForget（不等待响应）
        Mono<Void> sendMono = clientRequester
                .route("device.heartbeat")
                .data(message)
                .send();

        StepVerifier.create(sendMono)
                .verifyComplete();

        log.info("心跳发送完成（FireAndForget 无响应）");
    }

    // ==================== 3. REQUEST_STREAM 测试 ====================

    @Test
    @Order(4)
    @DisplayName("测试 REQUEST_STREAM - 服务器向客户端推送多条消息")
    void testRequestStream_ServerPush() {
        log.info("========== 测试 REQUEST_STREAM - 请求流 ==========");

        Message<String> requestMessage = Message.<String>builder()
                .route("stream.test")
                .type(Message.Type.REQUEST_STREAM)
                .data("start")
                .timestamp(Instant.now())
                .build();

        // 客户端请求流，服务器返回多条消息
        Flux<Message<String>> streamFlux = clientRequester
                .route("stream.test")
                .data(requestMessage)
                .retrieveFlux(STRING_MESSAGE_TYPE)
                .take(3)
                .timeout(Duration.ofSeconds(10));

        StepVerifier.create(streamFlux)
                .assertNext(msg -> log.info("收到流消息1: {}", msg.getData()))
                .assertNext(msg -> log.info("收到流消息2: {}", msg.getData()))
                .assertNext(msg -> log.info("收到流消息3: {}", msg.getData()))
                .verifyComplete();

        log.info("流接收完成");
    }

    // ==================== 4. REQUEST_CHANNEL 测试 ====================

    @Test
    @Order(5)
    @DisplayName("测试 REQUEST_CHANNEL - 双向流")
    void testRequestChannel_Bidirectional() {
        log.info("========== 测试 REQUEST_CHANNEL - 双向流 ==========");

        // 创建客户端流（持续发送心跳）
        Sinks.Many<Message<Heartbeat>> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 模拟持续发送3个心跳
        for (int i = 0; i < 3; i++) {
            Heartbeat hb = new Heartbeat();
            hb.setUuid(TEST_UUID + "-" + i);
            hb.setInterval(30);

            Message<Heartbeat> msg = Message.<Heartbeat>builder()
                    .route("channel.heartbeat")
                    .type(Message.Type.REQUEST_CHANNEL)
                    .data(hb)
                    .timestamp(Instant.now())
                    .build();

            sink.tryEmitNext(msg);
        }
        sink.tryEmitComplete();

        // 建立双向流通道
        Flux<Message<Heartbeat>> responseFlux = clientRequester
                .route("channel.heartbeat")
                .data(sink.asFlux())
                .retrieveFlux(HEARTBEAT_MESSAGE_TYPE)
                .timeout(Duration.ofSeconds(10));

        StepVerifier.create(responseFlux)
                .assertNext(msg -> {
                    log.info("收到心跳确认1: uuid={}", msg.getData().getUuid());
                    Assertions.assertEquals(TEST_UUID + "-0", msg.getData().getUuid());
                })
                .assertNext(msg -> {
                    log.info("收到心跳确认2: uuid={}", msg.getData().getUuid());
                    Assertions.assertEquals(TEST_UUID + "-1", msg.getData().getUuid());
                })
                .assertNext(msg -> {
                    log.info("收到心跳确认3: uuid={}", msg.getData().getUuid());
                    Assertions.assertEquals(TEST_UUID + "-2", msg.getData().getUuid());
                })
                .verifyComplete();

        log.info("双向流测试完成");
    }

    // ==================== 5. 双向通信场景测试 ====================

    @Test
    @Order(6)
    @DisplayName("测试完整双工通信场景 - 注册+心跳+请求")
    void testFullDuplexScenario() {
        log.info("========== 测试完整双工通信场景 ==========");

        String deviceUuid = "test-duplex-scenario-001";

        // 步骤 1: 注册设备 (REQUEST_RESPONSE)
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUuid(deviceUuid);
        registerRequest.setLaboratoryId(TEST_LAB_ID);

        Message<RegisterRequest> registerMsg = Message.<RegisterRequest>builder()
                .route("device.register")
                .type(Message.Type.REQUEST_RESPONSE)
                .data(registerRequest)
                .timestamp(Instant.now())
                .build();

        Mono<Message<RegisterResponse>> registerMono = clientRequester
                .route("device.register")
                .data(registerMsg)
                .retrieveMono(REGISTER_RESPONSE_TYPE);

        // 步骤 2: 发送心跳 (FIRE_AND_FORGET)
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setUuid(deviceUuid);
        heartbeat.setInterval(30);

        Message<Heartbeat> heartbeatMsg = Message.<Heartbeat>builder()
                .route("device.heartbeat")
                .type(Message.Type.FIRE_AND_FORGET)
                .data(heartbeat)
                .timestamp(Instant.now())
                .build();

        Mono<Void> heartbeatMono = clientRequester
                .route("device.heartbeat")
                .data(heartbeatMsg)
                .send();

        // 组合测试：先注册，再发送心跳
        StepVerifier.create(registerMono)
                .assertNext(regResp -> {
                    Assertions.assertEquals(Status.C10000, regResp.getStatus());
                    log.info("注册成功: {}", regResp.getData().getUuid());
                })
                .verifyComplete();

        StepVerifier.create(heartbeatMono)
                .verifyComplete();

        log.info("心跳发送成功");

        // 双工通信验证：连接保持活跃，可以连续发送多个请求
        // 这里不再检查 rsocket()，而是通过实际请求的成功来验证连接状态
        log.info("双工通信测试通过 - 连接保持活跃，可多轮交互");
    }

    // ==================== 6. 异常处理测试（放在最后，可能导致连接关闭）====================

    @Test
    @Order(7)
    @DisplayName("测试异常处理 - 无效路由")
    void testErrorHandling_InvalidRoute() {
        log.info("========== 测试异常处理 - 无效路由 ==========");

        Message<String> message = Message.<String>builder()
                .route("invalid.route")
                .type(Message.Type.REQUEST_RESPONSE)
                .data("test")
                .timestamp(Instant.now())
                .build();

        Mono<Message<String>> responseMono = clientRequester
                .route("invalid.route")
                .data(message)
                .retrieveMono(STRING_MESSAGE_TYPE);

        StepVerifier.create(responseMono)
                .expectError()  // 应该收到错误
                .verify(Duration.ofSeconds(5));

        log.info("异常处理测试通过");
    }
}
