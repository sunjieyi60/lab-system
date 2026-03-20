package xyz.jasenon.rsocket.classtimetablersocket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.core.protocol.Message;

import java.time.Instant;

/**
 * RSocket 设备通信测试
 * 
 * 测试使用 Message<T> 包装具体业务 DTO
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.rsocket.server.port=0"
})
public class ClassTimeTableRSocketTest {

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    /**
     * 测试设备注册流程
     * 
     * 使用 Message<RegisterRequest> 发送
     * 接收 Message<RegisterResponse>
     */
    @Test
    void testDeviceRegister() {
        RegisterRequest request = new RegisterRequest();
        request.setUuid("TEST001");
        request.setLaboratoryId(200L);

        Message<RegisterRequest> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(request);
        message.setTimestamp(Instant.now());

        Mono<Message<RegisterResponse>> result = requesterBuilder
                .tcp("localhost", 7000)
                .route("device.register")
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<>() {});

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    RegisterResponse payload = response.getPayload();
                    return payload != null 
                            && payload.getDeviceDbId() != null 
                            && payload.getConfig() != null;
                })
                .verifyComplete();
    }

    /**
     * 测试心跳流程
     * 
     * 使用 Message<HeartbeatPayload> 发送
     */
    @Test
    void testHeartbeat() {
        HeartbeatPayload payload = new HeartbeatPayload();
        payload.setDeviceDbId(1L);
        payload.setInterval(Const.Default.HEARTBEAT_INTERVAL);

        Message<HeartbeatPayload> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(payload);
        message.setTimestamp(Instant.now());

        Mono<Message<HeartbeatPayload>> result = requesterBuilder
                .tcp("localhost", 7000)
                .route(Const.Route.DEVICE_HEARTBEAT)
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<>() {});

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    HeartbeatPayload respPayload = response.getPayload();
                    return respPayload != null 
                            && respPayload.getInterval() != null
                            && respPayload.getInterval() == Const.Default.HEARTBEAT_INTERVAL;
                })
                .verifyComplete();
    }

    /**
     * 测试开门请求
     * 
     * 使用 Message<OpenDoorRequest> 发送
     * 接收 Message<OpenDoorResponse>
     */
    @Test
    void testOpenDoor() {
        OpenDoorRequest request = new OpenDoorRequest();
        request.setType(OpenDoorRequest.OpenType.REMOTE);
        request.setVerifyInfo("admin");
        request.setDuration(5);
        request.setRequestTime(Instant.now());

        Message<OpenDoorRequest> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(request);
        message.setTimestamp(Instant.now());

        Mono<Message<OpenDoorResponse>> result = requesterBuilder
                .tcp("localhost", 7000)
                .route("device.door.open")
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<>() {});

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    OpenDoorResponse payload = response.getPayload();
                    return payload != null && payload.isSuccess();
                })
                .verifyComplete();
    }

    /**
     * 测试更新配置请求
     * 
     * 使用 Message<UpdateConfigRequest> 发送
     * 接收 Message<UpdateConfigResponse>
     */
    @Test
    void testUpdateConfig() {
        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setConfig(Config.Default());
        request.setImmediate(true);
        request.setVersion(1L);
        request.setRequestTime(Instant.now());

        Message<UpdateConfigRequest> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(request);
        message.setTimestamp(Instant.now());

        Mono<Message<UpdateConfigResponse>> result = requesterBuilder
                .tcp("localhost", 7000)
                .route("device.config.update")
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<>() {});

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    UpdateConfigResponse payload = response.getPayload();
                    return payload != null && payload.isSuccess();
                })
                .verifyComplete();
    }

    /**
     * 测试重启请求
     * 
     * 使用 Message<RebootRequest> 发送
     * 接收 Message<RebootResponse>
     */
    @Test
    void testReboot() {
        RebootRequest request = new RebootRequest();
        request.setDelaySeconds(10);
        request.setReason("系统维护");
        request.setRequestTime(Instant.now());

        Message<RebootRequest> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(request);
        message.setTimestamp(Instant.now());

        Mono<Message<RebootResponse>> result = requesterBuilder
                .tcp("localhost", 7000)
                .route("device.reboot")
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<>() {});

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    RebootResponse payload = response.getPayload();
                    return payload != null && payload.isAccepted();
                })
                .verifyComplete();
    }

    /**
     * 测试截图请求
     * 
     * 使用 Message<ScreenshotRequest> 发送
     * 接收 Message<ScreenshotResponse>
     */
    @Test
    void testScreenshot() {
        ScreenshotRequest request = new ScreenshotRequest();
        request.setFormat("jpg");
        request.setQuality(80);
        request.setRequestTime(Instant.now());

        Message<ScreenshotRequest> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(request);
        message.setTimestamp(Instant.now());

        Mono<Message<ScreenshotResponse>> result = requesterBuilder
                .tcp("localhost", 7000)
                .route("device.screenshot")
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<>() {});

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    ScreenshotResponse payload = response.getPayload();
                    return payload != null && payload.isSuccess() && payload.getImageData() != null;
                })
                .verifyComplete();
    }

    /**
     * 测试更新课表请求
     * 
     * 使用 Message<UpdateScheduleRequest> 发送
     * 接收 Message<UpdateScheduleResponse>
     */
    @Test
    void testUpdateSchedule() {
        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setScheduleVersion(1L);
        request.setEffectiveTime(Instant.now());
        request.setRequestTime(Instant.now());

        Message<UpdateScheduleRequest> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(request);
        message.setTimestamp(Instant.now());

        Mono<Message<UpdateScheduleResponse>> result = requesterBuilder
                .tcp("localhost", 7000)
                .route("device.schedule.update")
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<>() {});

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    UpdateScheduleResponse payload = response.getPayload();
                    return payload != null && payload.isSuccess();
                })
                .verifyComplete();
    }

    /**
     * 测试更新人脸库请求
     * 
     * 使用 Message<UpdateFaceLibraryRequest> 发送
     * 接收 Message<UpdateFaceLibraryResponse>
     */
    @Test
    void testUpdateFaceLibrary() {
        UpdateFaceLibraryRequest request = new UpdateFaceLibraryRequest();
        request.setUpdateType(UpdateFaceLibraryRequest.UpdateType.INCREMENTAL);
        request.setLibraryVersion(1L);
        request.setRequestTime(Instant.now());

        Message<UpdateFaceLibraryRequest> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(request);
        message.setTimestamp(Instant.now());

        Mono<Message<UpdateFaceLibraryResponse>> result = requesterBuilder
                .tcp("localhost", 7000)
                .route("device.face-library.update")
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<>() {});

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    UpdateFaceLibraryResponse payload = response.getPayload();
                    return payload != null && payload.isSuccess();
                })
                .verifyComplete();
    }
}
