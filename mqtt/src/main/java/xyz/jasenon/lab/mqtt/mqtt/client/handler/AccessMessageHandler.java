package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import java.text.MessageFormat;

import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;

import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.common.entity.device.Access;
import xyz.jasenon.lab.common.entity.record.AccessRecord;
import xyz.jasenon.lab.common.utils.SumChecker;
import xyz.jasenon.lab.mqtt.mapper.AccessMapper;
import xyz.jasenon.lab.mqtt.mapper.AccessRecordMapper;

@Component
@Slf4j
public class AccessMessageHandler extends MqttMessageHandler<AccessMapper,AccessRecordMapper,Access,AccessRecord> {

    public AccessMessageHandler(AccessMapper deviceMapper, AccessRecordMapper recordMapper,
            RedissonClient redissonClient) {
        super(deviceMapper, recordMapper, redissonClient);
    }

    @Override 
    Access parse(byte[] payloads, Long rs485Id){
        Integer address = payloads[0] & 0xff;
        Integer selfId = payloads[2] & 0xff;

        Access access = new LambdaQueryChainWrapper<>(super.deviceMapper)
                .eq(Access::getSelfId, selfId)
                .eq(Access::getAddress, address)
                .eq(Access::getRs485GatewayId, rs485Id)
                .one();

        return access;
    }

    @Override
    public AccessRecord decryptPayload(byte[] payload, Long rs485Id){
        Integer address = payload[0] & 0xff;
        Integer selfId = payload[2] & 0xff;

        Boolean isOpen = payload[3] == (byte) 0xFF;
        Boolean isLcok = payload[5] == (byte) 0xFF;
        Integer lockStatus = switch (payload[4]) {
            case (byte) 0xFF -> 1;
            case 0x11 -> 2;
            case 0x00 -> 3;
            default -> null;
        };
        Integer delayTime = (int) payload[6];

        AccessRecord accessRecord = (AccessRecord) new AccessRecord()
                .address(address)
                .selfId(selfId)
                .isOpen(isOpen)
                .isLock(isLcok)
                .lockStatus(lockStatus)
                .delayTime(delayTime)
                .rs485Id(rs485Id);

        return accessRecord;
    }

    @Override
    boolean needSave(byte[] payload) {
        if (payload[1] == 0x0A) {
            log.info("地址:{}", payload[0]);
            log.info("用户操作成功");
            return false;
        }
        if (payload[1] == 0x03) {
            log.info("地址:{}", payload[0]);
            log.info("polling成功");
            return true;
        }
        throw new IllegalArgumentException(MessageFormat.format("未知的指令{0}", payload[1]));
    }

    @Override
    boolean verify(byte[] payload) {
        return SumChecker.verifyCheckSum(payload);
    }


}
