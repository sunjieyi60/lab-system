package xyz.jasenon.lab.class_time_table.rsocket;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.jasenon.lab.class_time_table.rsocket.device.ClassTimeTableDevice;
import xyz.jasenon.lab.class_time_table.rsocket.model.AccessRecord;
import xyz.jasenon.lab.class_time_table.rsocket.model.FaceData;
import xyz.jasenon.lab.class_time_table.rsocket.model.RpcMessage;
import xyz.jasenon.lab.class_time_table.rsocket.model.TimeTable;
import xyz.jasenon.lab.class_time_table.rsocket.protocol.Command;
import xyz.jasenon.lab.class_time_table.rsocket.model.StatusCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RSocket P2P 通信测试
 */
@Slf4j
public class RSocketPeerTest {

    /**
     * 测试班牌设备作为服务端启动
     */
    @Test
    public void testDeviceAsServer() throws InterruptedException {
        ClassTimeTableDevice device = new ClassTimeTableDevice(
                "device-001", 
                "A101班牌", 
                "A101"
        );
        
        // 设备作为服务端启动（等待管理端连接）
        device.startAsServer(9000)
                .doOnSuccess(v -> log.info("设备服务端启动成功"))
                .subscribe();
        
        // 保持运行
        Thread.sleep(60000);
    }
    
    /**
     * 测试班牌设备作为客户端连接管理端
     */
    @Test
    public void testDeviceAsClient() throws InterruptedException {
        ClassTimeTableDevice device = new ClassTimeTableDevice(
                "device-002", 
                "A102班牌", 
                "A102"
        );
        
        // 设备作为客户端连接到管理端
        device.connectToManager("localhost", 9001)
                .doOnSuccess(v -> {
                    log.info("设备连接管理端成功");
                    
                    // 测试主动获取课表
                    device.getRpcClient()
                            .getCurrentTimeTable(device.getUuid())
                            .subscribe(resp -> {
                                log.info("获取课表响应: {}", resp);
                                if (resp.isSuccess()) {
                                    log.info("当前课程: {}", resp.getPayload().getCourseName());
                                } else if (resp.getStatus() == StatusCode.TIME_TABLE_EMPTY) {
                                    log.info("当前无课程");
                                }
                            });
                })
                .subscribe();
        
        // 保持运行一段时间
        Thread.sleep(30000);
        
        device.disconnect().subscribe();
    }
    
    /**
     * 测试下发人脸数据
     */
    @Test
    public void testPushFaceData() {
        ClassTimeTableDevice device = new ClassTimeTableDevice(
                "device-003", 
                "A103班牌", 
                "A103"
        );
        
        // 先启动设备作为服务端
        device.startAsServer(9002).subscribe();
        
        // 构造人脸数据
        FaceData faceData = FaceData.builder()
                .faceId("face-001")
                .faceName("张三")
                .userId("student-001")
                .userType(FaceData.UserType.STUDENT)
                .featureData("base64encodedfeaturedata...")
                .build();
        
        // 测试下发人脸
        Mono<RpcMessage<String>> result = device.pushFaceData(faceData);
        
        StepVerifier.create(result)
                .expectNextMatches(resp -> resp.isSuccess() && resp.getCommand() == Command.PUSH_FACE_DATA)
                .verifyComplete();
    }
    
    /**
     * 测试上报访问记录
     */
    @Test
    public void testReportAccess() {
        ClassTimeTableDevice device = new ClassTimeTableDevice(
                "device-004", 
                "A104班牌", 
                "A104"
        );
        
        // 连接到管理端
        device.connectToManager("localhost", 9001)
                .doOnSuccess(v -> {
                    // 上报访问记录
                    device.reportFaceAccess(
                                    "face-001",
                                    "张三",
                                    AccessRecord.Result.ALLOWED,
                                    95
                            )
                            .doOnSuccess(r -> log.info("访问记录上报成功"))
                            .subscribe();
                })
                .subscribe();
    }
    
    /**
     * 测试 Command 枚举
     */
    @Test
    public void testCommandEnum() {
        // 测试从字符串获取枚举
        Command cmd = Command.fromCode("pushFaceData");
        assert cmd == Command.PUSH_FACE_DATA;
        
        // 测试命令分类
        assert Command.PUSH_FACE_DATA.isFaceRelated();
        assert Command.GET_CURRENT_TIME_TABLE.isTimeTableRelated();
        assert Command.HEARTBEAT.isFireAndForget();
        assert Command.SUBSCRIBE_TIME_TABLE_UPDATES.isStream();
        
        log.info("Command 枚举测试通过");
    }
    
    /**
     * 测试 StatusCode 枚举
     */
    @Test
    public void testStatusCodeEnum() {
        // 测试状态判断
        assert StatusCode.SUCCESS.isSuccess();
        assert StatusCode.BAD_REQUEST.isClientError();
        assert StatusCode.INTERNAL_ERROR.isServerError();
        assert StatusCode.DEVICE_BUSY.isBusinessError();
        
        // 测试从 code 获取枚举
        assert StatusCode.fromCode(0) == StatusCode.SUCCESS;
        assert StatusCode.fromCode(404) == StatusCode.NOT_FOUND;
        
        log.info("StatusCode 枚举测试通过");
    }
    
    /**
     * 测试 RpcMessage 构造
     */
    @Test
    public void testRpcMessageBuilder() {
        // 测试成功消息
        RpcMessage<String> successMsg = RpcMessage.success(Command.PUSH_FACE_DATA, "注册成功");
        assert successMsg.isSuccess();
        assert successMsg.getCommand() == Command.PUSH_FACE_DATA;
        assert successMsg.getStatus() == StatusCode.SUCCESS;
        
        // 测试错误消息
        RpcMessage<String> errorMsg = RpcMessage.error(Command.PUSH_FACE_DATA, StatusCode.FACE_REGISTER_FAILED, "人脸质量不合格");
        assert !errorMsg.isSuccess();
        assert errorMsg.getStatus() == StatusCode.FACE_REGISTER_FAILED;
        
        log.info("RpcMessage 构造测试通过");
    }
}
