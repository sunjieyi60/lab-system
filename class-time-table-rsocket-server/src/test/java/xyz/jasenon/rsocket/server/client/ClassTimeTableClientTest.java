package xyz.jasenon.rsocket.server.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.jasenon.rsocket.core.packet.*;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.client.Client;

import java.time.Instant;

/**
 * 智慧班牌 RSocket 客户端单元测试
 *
 * 测试服务端功能：
 * 1. 设备注册
 * 2. 设备心跳
 * 3. 消息响应格式
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "rsocket.server.port=0",  // 随机端口
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.sql.init.schema-locations=classpath:db/schema-h2.sql"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClassTimeTableClientTest {

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Autowired
    private Client client;

    private static String testUuid = "test-device-001";
    private static final Long TEST_LAB_ID = 1L;

    /**
     * 测试 1: 设备注册请求
     */
    @Test
    @Order(1)
    void testDeviceRegister() {
        log.info("========== 测试设备注册 ==========");

        // 构建注册请求
        RegisterRequest request = new RegisterRequest();
        request.setUuid(testUuid);
        request.setLaboratoryId(TEST_LAB_ID);

        // 使用 Client 发送请求（类型安全方式）
        Mono<Message<RegisterResponse>> responseMono = client.send(request);

        // 验证响应
        StepVerifier.create(responseMono)
                .assertNext(msg -> {
                    log.info("收到注册响应: status={}, data={}", msg.getStatus(), msg.getData());
                    
                    // 验证响应状态
                    Assertions.assertNotNull(msg.getStatus());
                    Assertions.assertEquals(Status.C10000, msg.getStatus());
                    
                    // 验证响应数据
                    Assertions.assertNotNull(msg.getData());
                    RegisterResponse response = msg.getData();
                    Assertions.assertEquals(testUuid, response.getUuid());
                    Assertions.assertNotNull(response.getConfig());
                    
                    log.info("设备 {} 注册成功", response.getUuid());
                })
                .verifyComplete();
    }

    /**
     * 测试 2: 使用 MessageAdaptor 方式注册
     */
    @Test
    @Order(2)
    void testDeviceRegisterWithAdaptor() {
        log.info("========== 测试 MessageAdaptor 注册 ==========");

        testUuid = "test-device-002";

        // 创建实现 MessageAdaptor 的请求对象
        RegisterRequestAdaptor request = new RegisterRequestAdaptor(testUuid, TEST_LAB_ID);

        // 使用 Client 发送请求（类型安全方式）
        Mono<Message<RegisterResponse>> responseMono = client.send(request);

        // 验证响应
        StepVerifier.create(responseMono)
                .assertNext(msg -> {
                    log.info("收到 Adaptor 注册响应: uuid={}", msg.getData().getUuid());
                    Assertions.assertEquals(testUuid, msg.getData().getUuid());
                    Assertions.assertNotNull(msg.getData().getConfig());
                })
                .verifyComplete();
    }

    /**
     * 测试 3: 设备心跳（FireAndForget）
     */
    @Test
    @Order(3)
    void testDeviceHeartbeat() {
        log.info("========== 测试设备心跳 ==========");

        // 先注册设备
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUuid(testUuid);
        registerRequest.setLaboratoryId(TEST_LAB_ID);

        Message<RegisterRequest> registerMessage = Message.<RegisterRequest>builder()
                .route("device.register")
                .data(registerRequest)
                .timestamp(Instant.now())
                .build();

        // 注册后发送心跳
        client.send(registerMessage)
                .flatMap(resp -> {
                    log.info("注册成功，发送心跳");

                    // 构建心跳请求
                    Heartbeat heartbeat = new Heartbeat();
                    heartbeat.setUuid(testUuid);
                    heartbeat.setInterval(30);
                    heartbeat.setConfigUpdated(false);

                    Message<Heartbeat> heartbeatMessage = Message.<Heartbeat>builder()
                            .route("device.heartbeat")
                            .data(heartbeat)
                            .timestamp(Instant.now())
                            .build();

                    // FireAndForget 发送心跳（不等待响应）
                    return client.sendAndForget(heartbeatMessage)
                            .doOnSuccess(v -> log.info("心跳发送成功"));
                })
                .as(StepVerifier::create)
                .verifyComplete();
    }

    /**
     * 测试 4: 重复注册（设备已存在）
     */
    @Test
    @Order(4)
    void testDuplicateRegister() {
        log.info("========== 测试重复注册 ==========");

        // 使用相同的 UUID 再次注册
        RegisterRequest request = new RegisterRequest();
        request.setUuid(testUuid);
        request.setLaboratoryId(2L);  // 不同的实验室ID

        Mono<Message<RegisterResponse>> responseMono = client.send(request);

        StepVerifier.create(responseMono)
                .assertNext(msg -> {
                    log.info("重复注册响应: uuid={}, config={}", 
                            msg.getData().getUuid(), 
                            msg.getData().getConfig());
                    
                    Assertions.assertEquals(testUuid, msg.getData().getUuid());
                    Assertions.assertNotNull(msg.getData().getConfig());
                })
                .verifyComplete();
    }

    /**
     * 测试 5: 连接状态检查
     */
    @Test
    @Order(5)
    void testConnectionStatus() {
        log.info("========== 测试连接状态 ==========");

        // 验证客户端连接状态
        boolean connected = client.isConnected();
        log.info("客户端连接状态: {}", connected);

        // 连接应该已建立（因为之前的测试已经使用过）
        Assertions.assertTrue(connected, "客户端应该已连接");
    }

    // ==================== 辅助类 ====================

    /**
     * 用于测试的 RegisterRequest MessageAdaptor 实现
     */
    static class RegisterRequestAdaptor implements xyz.jasenon.rsocket.core.protocol.MessageAdaptor<RegisterRequest, RegisterResponse> {
        
        private final String uuid;
        private final Long laboratoryId;

        public RegisterRequestAdaptor(String uuid, Long laboratoryId) {
            this.uuid = uuid;
            this.laboratoryId = laboratoryId;
        }

        @Override
        public Message<RegisterRequest> adaptor() {
            RegisterRequest request = new RegisterRequest();
            request.setUuid(uuid);
            request.setLaboratoryId(laboratoryId);

            return Message.<RegisterRequest>builder()
                    .route("device.register")
                    .data(request)
                    .timestamp(Instant.now())
                    .build();
        }

        @Override
        public Class<RegisterResponse> getResponseType() {
            return RegisterResponse.class;
        }
    }
}
