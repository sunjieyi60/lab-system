package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.AirCondition;
import xyz.jasenon.lab.common.entity.record.AirConditionRecord;
import xyz.jasenon.lab.common.utils.SumChecker;
import xyz.jasenon.lab.mqtt.mapper.AirConditionMapper;
import xyz.jasenon.lab.mqtt.mapper.AirConditionRecordMapper;

import java.util.List;

@Component
public class AirConditionMessageHandler
        extends MqttMessageHandler<AirConditionMapper, AirConditionRecordMapper, AirCondition, AirConditionRecord> {

    private static final Logger log = LoggerFactory.getLogger(AirConditionMessageHandler.class);

    public AirConditionMessageHandler(AirConditionMapper deviceMapper, AirConditionRecordMapper recordMapper,
                                      RedissonClient redissonClient) {
        super(deviceMapper, recordMapper, redissonClient);
    }

    @Override
    AirConditionRecord decryptPayload(byte[] payload, Long rs485Id) {
        Integer address = payload[0] & 0xFF;
        Integer selfId = payload[1] & 0xFF;

        Boolean isOpen = payload[2] == 0x01;
        String mode = switch (payload[3]) {
            case 0x01 -> "制热";
            case 0x02 -> "制冷";
            case 0x04 -> "送风";
            case 0x08 -> "除湿";
            default -> null;
        };
        Integer temperature = payload[4] & 0xFF;
        String speed = switch (payload[5]) {
            case 0x00 -> "自动";
            case 0x01 -> "低速";
            case 0x02 -> "中速";
            case 0x03 -> "高速";
            default -> null;
        };
        Integer roomTemperature = (int) payload[6];
        if (mode == null || speed == null) {
            return null;
        }
        Integer errorCode = payload[7] & 0xFF;

        AirConditionRecord record = new AirConditionRecord()
                .address(address)
                .selfId(selfId)
                .isOpen(isOpen)
                .mode(mode)
                .speed(speed)
                .temperature(temperature)
                .roomTemperature(roomTemperature)
                .errorCode(errorCode);

        return record;
    }

    @Override
    AirCondition parse(byte[] payload, Long rs485Id) {
        Integer address = payload[0] & 0xFF;
        Integer selfId = payload[1] & 0xFF;

        AirCondition airCondition = new LambdaQueryChainWrapper<>(super.deviceMapper)
                .eq(AirCondition::getAddress, address)
                .eq(AirCondition::getSelfId, selfId)
                .eq(AirCondition::getRs485GatewayId, rs485Id)
                .one();

        return airCondition;
    }

    @Override
    boolean needSave(byte[] payload) {
        Integer address = payload[0] & 0xFF;
        Integer selfIdOrFunctionCode = payload[1] & 0xFF;
        boolean isFunctionCode = List.of(0xBB, 0xCC).contains(selfIdOrFunctionCode);
        if (isFunctionCode) {
            log.info("设备地址: {}, 功能码: {}", address, selfIdOrFunctionCode);
            return false;
        }
        Integer selfId = payload[1] & 0xFF;
        log.info("设备地址: {}, 编码: {}", address, selfId);
        return true;
    }

    @Override
    boolean verify(byte[] payload) {
        return SumChecker.verifyCheckSum(payload);
    }

}
