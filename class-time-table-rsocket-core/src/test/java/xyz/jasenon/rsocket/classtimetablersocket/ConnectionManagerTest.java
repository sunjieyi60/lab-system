package xyz.jasenon.rsocket.classtimetablersocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.jasenon.rsocket.classtimetablersocket.dto.OpenDoorRequest;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.rsocket.ConnectionManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 设备连接管理器测试
 * 
 * 测试 RSocket 原生请求-响应匹配功能
 */
public class ConnectionManagerTest {

    private ConnectionManager connectionManager;
    private RSocketRequester mockRequester;

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();
        mockRequester = mock(RSocketRequester.class);
        
        RSocketRequester.RSocketRequesterClient mockClient = mock(RSocketRequester.RSocketRequesterClient.class);
        io.rsocket.RSocket mockRSocket = mock(io.rsocket.RSocket.class);
        
        when(mockRequester.rsocketClient()).thenReturn(mockClient);
        when(mockClient.source()).thenReturn(Mono.just(mockRSocket));
        when(mockRSocket.onClose()).thenReturn(Mono.empty());
        when(mockRSocket.isDisposed()).thenReturn(false);
    }

    @Test
    void testRegisterConnection() {
        Long deviceDbId = 1L;
        
        connectionManager.registerConnection(deviceDbId, mockRequester);
        
        assertTrue(connectionManager.isOnline(deviceDbId));
        assertEquals(1, connectionManager.getOnlineCount());
    }

    @Test
    void testRemoveConnection() {
        Long deviceDbId = 1L;
        
        connectionManager.registerConnection(deviceDbId, mockRequester);
        connectionManager.removeConnection(deviceDbId);
        
        assertFalse(connectionManager.isOnline(deviceDbId));
        assertEquals(0, connectionManager.getOnlineCount());
    }

    @Test
    void testIsOnline_NullDevice() {
        assertFalse(connectionManager.isOnline(null));
    }

    @Test
    void testBroadcastRequest_NoDevices() {
        OpenDoorRequest request = new OpenDoorRequest();
        request.setType(OpenDoorRequest.OpenType.REMOTE);
        
        Message<OpenDoorRequest> message = new Message<>();
        message.setType(Message.Type.FIRE_AND_FORGET);
        message.setPayload(request);
        
        StepVerifier.create(connectionManager.broadcastRequest(message, "device.door.open"))
                .expectNext(0)
                .verifyComplete();
    }

    @Test
    void testRequestResponse_DeviceOffline() {
        Long deviceDbId = 999L; // 未注册的设备
        
        OpenDoorRequest request = new OpenDoorRequest();
        request.setType(OpenDoorRequest.OpenType.REMOTE);
        
        Message<OpenDoorRequest> message = new Message<>();
        message.setPayload(request);
        
        // 应该返回错误，因为设备不在线
        StepVerifier.create(connectionManager.requestResponse(deviceDbId, message, "device.door.open"))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
