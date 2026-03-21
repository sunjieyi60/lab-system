package xyz.jasenon.rsocket.server.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.client.Client;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 智慧班牌 RSocket 客户端 Mock 单元测试
 *
 * 使用 Mockito 模拟 Client 行为，测试消息构造和响应处理
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClassTimeTableClientMockTest {

    @Mock
    private Client client;

    private static final String TEST_UUID = "test-device-001";
    private static final Long TEST_LAB_ID = 1L;

    /**
     * 测试 1: 验证注册请求消息构造
     */
    @Test
    @Order(1)
    void testRegisterRequestMessage() {
        log.info("========== 测试注册请求消息构造 ==========");

        // 构建注册请求
        RegisterRequest request = new RegisterRequest();
        request.setUuid(TEST_UUID);
        request.setLaboratoryId(TEST_LAB_ID);

        // 构建 Message
        Message<RegisterRequest> message = Message.<RegisterRequest>builder()
                .route("device.register")
                .data(request)
                .timestamp(Instant.now())
                .build();

        // 验证消息内容
        Assertions.assertNotNull(message);
        Assertions.assertEquals("device.register", message.getRoute());
        Assertions.assertNotNull(message.getData());
        Assertions.assertEquals(TEST_UUID, message.getData().getUuid());
        Assertions.assertEquals(TEST_LAB_ID, message.getData().getLaboratoryId());
        Assertions.assertNotNull(message.getTimestamp());

        log.info("注册请求消息构造成功: uuid={}, labId={}", 
                message.getData().getUuid(), 
                message.getData().getLaboratoryId());
    }

    /**
     * 测试 2: 验证注册响应消息构造
     */
    @Test
    @Order(2)
    void testRegisterResponseMessage() {
        log.info("========== 测试注册响应消息构造 ==========");

        // 构建注册响应
        RegisterResponse response = new RegisterResponse();
        response.setUuid(TEST_UUID);
        response.setConfig(Config.Default());

        // 构建响应 Message（服务端返回格式）
        Message<RegisterResponse> responseMessage = Message.<RegisterResponse>builder()
                .from("SERVER")
                .to(TEST_UUID)
                .data(response)
                .status(Status.C10000)
                .timestamp(Instant.now())
                .build();

        // 验证响应消息
        Assertions.assertNotNull(responseMessage);
        Assertions.assertEquals("SERVER", responseMessage.getFrom());
        Assertions.assertEquals(TEST_UUID, responseMessage.getTo());
        Assertions.assertEquals(Status.C10000, responseMessage.getStatus());
        Assertions.assertNotNull(responseMessage.getData());
        Assertions.assertEquals(TEST_UUID, responseMessage.getData().getUuid());
        Assertions.assertNotNull(responseMessage.getData().getConfig());

        log.info("注册响应消息构造成功: status={}, config={}", 
                responseMessage.getStatus(), 
                responseMessage.getData().getConfig());
    }

    /**
     * 测试 3: 使用 MessageAdaptor 构造请求
     */
    @Test
    @Order(3)
    void testMessageAdaptor() {
        log.info("========== 测试 MessageAdaptor ==========");

        // 创建 Adaptor
        RegisterRequestAdaptor adaptor = new RegisterRequestAdaptor(TEST_UUID, TEST_LAB_ID);

        // 获取 Message
        Message<RegisterRequest> message = adaptor.adaptor();

        // 验证
        Assertions.assertNotNull(message);
        Assertions.assertEquals("device.register", message.getRoute());
        Assertions.assertEquals(TEST_UUID, message.getData().getUuid());

        // 验证响应类型
        Class<RegisterResponse> responseType = adaptor.getResponseType();
        Assertions.assertEquals(RegisterResponse.class, responseType);

        log.info("MessageAdaptor 工作正常: route={}, responseType={}", 
                message.getRoute(), responseType.getSimpleName());
    }

    /**
     * 测试 4: Mock 客户端发送注册请求
     */
    @Test
    @Order(4)
    @SuppressWarnings("unchecked")
    void testMockClientRegister() {
        log.info("========== 测试 Mock 客户端注册 ==========");

        // 准备模拟响应
        RegisterResponse mockResponse = new RegisterResponse();
        mockResponse.setUuid(TEST_UUID);
        mockResponse.setConfig(Config.Default());

        Message<RegisterResponse> mockResponseMessage = Message.<RegisterResponse>builder()
                .data(mockResponse)
                .status(Status.C10000)
                .timestamp(Instant.now())
                .build();

        // 配置 Mock
        when(client.send(any(Message.class))).thenReturn(Mono.just(mockResponseMessage));

        // 构建请求
        RegisterRequest request = new RegisterRequest();
        request.setUuid(TEST_UUID);
        request.setLaboratoryId(TEST_LAB_ID);

        // 发送请求并验证
        Mono<Message<RegisterResponse>> result = client.send(request);

        StepVerifier.create(result)
                .assertNext(msg -> {
                    Assertions.assertEquals(Status.C10000, msg.getStatus());
                    Assertions.assertEquals(TEST_UUID, msg.getData().getUuid());
                    log.info("Mock 注册成功: uuid={}, config={}", 
                            msg.getData().getUuid(), 
                            msg.getData().getConfig());
                })
                .verifyComplete();
    }

    /**
     * 测试 5: Mock 客户端使用 MessageAdaptor
     */
    @Test
    @Order(5)
    @SuppressWarnings("unchecked")
    void testMockClientWithAdaptor() {
        log.info("========== 测试 Mock 客户端使用 Adaptor ==========");

        // 准备模拟响应
        RegisterResponse mockResponse = new RegisterResponse();
        mockResponse.setUuid(TEST_UUID);
        mockResponse.setConfig(Config.Default());

        Message<RegisterResponse> mockResponseMessage = Message.<RegisterResponse>builder()
                .data(mockResponse)
                .status(Status.C10000)
                .timestamp(Instant.now())
                .build();

        // 配置 Mock
        when(client.send(any(MessageAdaptor.class))).thenReturn(Mono.just(mockResponseMessage));

        // 使用 Adaptor
        RegisterRequestAdaptor adaptor = new RegisterRequestAdaptor(TEST_UUID, TEST_LAB_ID);

        // 发送请求并验证
        Mono<Message<RegisterResponse>> result = client.send(adaptor);

        StepVerifier.create(result)
                .assertNext(msg -> {
                    Assertions.assertEquals(Status.C10000, msg.getStatus());
                    Assertions.assertNotNull(msg.getData().getConfig());
                    log.info("Mock Adaptor 调用成功: status={}", msg.getStatus());
                })
                .verifyComplete();
    }

    /**
     * 测试 6: 验证 FireAndForget 消息构造
     */
    @Test
    @Order(6)
    void testFireAndForgetMessage() {
        log.info("========== 测试 FireAndForget 消息构造 ==========");

        // 构建心跳消息（FireAndForget 类型）
        xyz.jasenon.rsocket.core.packet.Heartbeat heartbeat = 
                new xyz.jasenon.rsocket.core.packet.Heartbeat();
        heartbeat.setUuid(TEST_UUID);
        heartbeat.setInterval(30);
        heartbeat.setConfigUpdated(false);

        Message<xyz.jasenon.rsocket.core.packet.Heartbeat> message = 
                Message.<xyz.jasenon.rsocket.core.packet.Heartbeat>builder()
                        .route("device.heartbeat")
                        .type(Message.Type.FIRE_AND_FORGET)
                        .data(heartbeat)
                        .timestamp(Instant.now())
                        .build();

        // 验证
        Assertions.assertNotNull(message);
        Assertions.assertEquals("device.heartbeat", message.getRoute());
        Assertions.assertEquals(Message.Type.FIRE_AND_FORGET, message.getType());
        Assertions.assertEquals(TEST_UUID, message.getData().getUuid());

        log.info("FireAndForget 消息构造成功: route={}, type={}", 
                message.getRoute(), message.getType());
    }

    // ==================== 辅助类 ====================

    /**
     * 用于测试的 RegisterRequest MessageAdaptor 实现
     */
    static class RegisterRequestAdaptor implements MessageAdaptor<RegisterRequest, RegisterResponse> {
        
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
