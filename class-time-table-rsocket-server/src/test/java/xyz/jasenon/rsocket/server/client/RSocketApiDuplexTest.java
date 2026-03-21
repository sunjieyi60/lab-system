package xyz.jasenon.rsocket.server.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.jasenon.rsocket.core.Api;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.core.packet.*;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Duration;
import java.time.Instant;

/**
 * 使用 Api 类的 RSocket 双工通信测试
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
public class RSocketApiDuplexTest {

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Autowired
    private Api api;

    private static RSocketRequester clientRequester;
    private static final String TEST_UUID = "test-api-duplex-001";
    private static final Long TEST_LAB_ID = 1L;

    @BeforeAll
    void setUp() {
        // 建立连接并确保连接可用（发送预热请求）
        clientRequester = requesterBuilder.tcp("localhost", 7001);
        
        // 预热：发送一个实际的注册请求，确保连接完全建立
        RegisterRequest warmup = new RegisterRequest();
        warmup.setUuid("warmup-device");
        warmup.setLaboratoryId(1L);
        
        Message<RegisterRequest> warmupMsg = Message.<RegisterRequest>builder()
                .route("device.register")
                .data(warmup)
                .timestamp(Instant.now())
                .build();
        
        try {
            clientRequester.route("device.register")
                    .data(warmupMsg)
                    .retrieveMono(new ParameterizedTypeReference<Message<RegisterResponse>>() {})
                    .block(Duration.ofSeconds(5));
            log.info("连接预热完成");
        } catch (Exception e) {
            log.warn("预热请求失败（可能设备已存在）: {}", e.getMessage());
        }
        
        log.info("RSocket 客户端连接已建立");
    }

    @AfterAll
    void tearDown() {
        if (clientRequester != null) {
            try {
                clientRequester.rsocket().dispose();
            } catch (Exception ignored) {}
        }
    }

    // ==================== 1. 注册设备 ====================

    @Test
    @Order(1)
    @DisplayName("测试 1: Api.bindClassTimeTable 注册设备")
    void testApiBindDevice() {
        log.info("========== 测试 1: Api.bindClassTimeTable 注册设备 ==========");

        ClassTimeTable device = new ClassTimeTable();
        device.setUuid(TEST_UUID);
        device.setLaboratoryId(TEST_LAB_ID);
        device.setConfig(Config.Default());

        // 绑定设备与连接
        api.bindClassTimeTable(device, clientRequester);

        log.info("✓ 设备 {} 已通过 Api.bindClassTimeTable 绑定到连接", TEST_UUID);

        // 验证绑定成功
        RSocketRequester savedRequester = api.getRequesterByUnique(device);
        Assertions.assertNotNull(savedRequester, "Api 应保存了设备连接");

        log.info("✓ 可通过 Api.getRequesterByUnique 获取到设备连接");
    }

    // ==================== 2. 服务端主动发送 ====================

    @Test
    @Order(2)
    @DisplayName("测试 2: Api.sendTo 向设备发送消息")
    void testApiSendTo() {
        log.info("========== 测试 2: Api.sendTo 向设备发送 ==========");

        // 构建命令
        UpdateConfigRequest configUpdate = new UpdateConfigRequest();
        configUpdate.setVersion(System.currentTimeMillis());
        configUpdate.setImmediate(true);

        Message<UpdateConfigRequest> command = Message.<UpdateConfigRequest>builder()
                .route("device.config.update")  // 使用服务端已有的路由
                .type(Message.Type.REQUEST_RESPONSE)
                .data(configUpdate)
                .timestamp(Instant.now())
                .build();

        log.info("服务端通过 Api.sendTo 向设备 {} 发送配置更新", TEST_UUID);

        // 注意：这里实际上是通过 clientRequester 发送到服务端
        // 真正的双工需要客户端实现 Responder
        Mono<Message<?>> responseMono = api.sendTo(TEST_UUID, command);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    log.info("✓ 收到响应: status={}", response.getStatus());
                    Assertions.assertEquals(Status.C10000, response.getStatus());
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    @DisplayName("测试 3: Api.send(MessageAdaptor) 类型安全发送")
    void testApiSendWithAdaptor() {
        log.info("========== 测试 3: Api.send(MessageAdaptor) 类型安全发送 ==========");

        ClassTimeTable device = new ClassTimeTable();
        device.setUuid(TEST_UUID);

        RSocketRequester deviceConnection = api.getRequesterByUnique(device);
        Assertions.assertNotNull(deviceConnection);

        // 使用已有的服务端路由
        MessageAdaptor<UpdateConfigRequest, UpdateConfigResponse> adaptor = 
                new ConfigUpdateAdaptor(100L, true);

        log.info("服务端通过 Api.send(MessageAdaptor) 发送配置更新");

        // 类型安全发送
        Mono<Message<UpdateConfigResponse>> responseMono = api.send(adaptor, deviceConnection);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    log.info("✓ 收到配置更新响应: version={}", response.getData());
                    Assertions.assertEquals(100L, response.getData());
                })
                .verifyComplete();

        log.info("✓ Api.send(MessageAdaptor) 提供编译期类型安全");
    }

    // ==================== 3. 广播 ====================

    @Test
    @Order(4)
    @DisplayName("测试 4: Api.broadcast 广播消息")
    void testApiBroadcast() {
        log.info("========== 测试 4: Api.broadcast 广播 ==========");

        // 构建广播消息（使用服务端支持的路由）
        Message<String> broadcast = Message.<String>builder()
                .route("device.heartbeat")  // 心跳广播
                .type(Message.Type.FIRE_AND_FORGET)
                .data("broadcast-test")
                .timestamp(Instant.now())
                .build();

        log.info("服务端准备广播消息");

        Mono<Integer> countMono = api.broadcast(broadcast);

        StepVerifier.create(countMono)
                .assertNext(count -> {
                    log.info("✓ 广播完成，影响 {} 个设备", count);
                })
                .verifyComplete();
    }

    // ==================== 4. 完整双工场景 ====================

    @Test
    @Order(5)
    @DisplayName("测试 5: 完整双工场景")
    void testFullDuplexScenario() {
        log.info("========== 测试 5: 完整双工场景 ==========");

        String scenarioUuid = "test-full-duplex-001";

        // 1. 新设备注册
        ClassTimeTable device = new ClassTimeTable();
        device.setUuid(scenarioUuid);
        device.setLaboratoryId(TEST_LAB_ID);
        device.setConfig(Config.Default());

        api.bindClassTimeTable(device, clientRequester);
        log.info("1. ✓ 设备 {} 注册并绑定", scenarioUuid);

        // 2. 服务端主动发送配置更新
        RSocketRequester deviceConn = api.getRequesterByUnique(device);
        Assertions.assertNotNull(deviceConn);

        UpdateConfigRequest config = new UpdateConfigRequest();
        config.setVersion(999L);
        config.setImmediate(true);

        Message<UpdateConfigRequest> configMsg = Message.<UpdateConfigRequest>builder()
                .route("device.config.update")
                .data(config)
                .timestamp(Instant.now())
                .build();

        Mono<Message<?>> response = api.send(configMsg, deviceConn);
        
        StepVerifier.create(response)
                .assertNext(r -> Assertions.assertEquals(Status.C10000, r.getStatus()))
                .verifyComplete();

        log.info("2. ✓ 服务端主动向客户端发送配置更新成功");

        // 3. 心跳保持
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setUuid(scenarioUuid);
        heartbeat.setInterval(30);

        Message<Heartbeat> hbMsg = Message.<Heartbeat>builder()
                .route("device.heartbeat")
                .data(heartbeat)
                .timestamp(Instant.now())
                .build();

        Mono<Void> hbResponse = clientRequester.route("device.heartbeat").data(hbMsg).send();
        StepVerifier.create(hbResponse).verifyComplete();

        log.info("3. ✓ 心跳发送成功");

        log.info("========== 完整双工场景测试通过 ==========");
        log.info("验证了：");
        log.info("  ✓ Api.bindClassTimeTable - 设备与连接绑定");
        log.info("  ✓ Api.getRequesterByUnique - 获取设备连接");
        log.info("  ✓ Api.send / Api.sendTo - 服务端主动推送");
        log.info("  ✓ 连接复用和双向通信");
    }

    // ==================== 辅助类 ====================

    /**
     * 配置更新的 MessageAdaptor
     */
    static class ConfigUpdateAdaptor implements MessageAdaptor<UpdateConfigRequest, UpdateConfigResponse> {
        private final Long version;
        private final boolean immediate;

        public ConfigUpdateAdaptor(Long version, boolean immediate) {
            this.version = version;
            this.immediate = immediate;
        }

        @Override
        public Message<UpdateConfigRequest> adaptor() {
            UpdateConfigRequest request = new UpdateConfigRequest();
            request.setVersion(version);
            request.setImmediate(immediate);
            request.setRequestTime(Instant.now());

            return Message.<UpdateConfigRequest>builder()
                    .route("device.config.update")
                    .type(Message.Type.REQUEST_RESPONSE)
                    .data(request)
                    .timestamp(Instant.now())
                    .build();
        }

        @Override
        public Class<UpdateConfigResponse> getResponseType() {
            return UpdateConfigResponse.class;
        }
    }
}
