package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import org.redisson.api.RedissonClient;

import xyz.jasenon.lab.common.entity.device.AirCondition;
import xyz.jasenon.lab.common.entity.record.AirConditionRecord;
import xyz.jasenon.lab.common.utils.SumChecker;
import xyz.jasenon.lab.mqtt.mapper.AirConditionMapper;
import xyz.jasenon.lab.mqtt.mapper.AirConditionRecordMapper;

public class AirConditionMessageHandler
        extends MqttMessageHandler<AirConditionMapper, AirConditionRecordMapper, AirCondition, AirConditionRecord> {

    public AirConditionMessageHandler(AirConditionMapper deviceMapper, AirConditionRecordMapper recordMapper,
            RedissonClient redissonClient) {
        super(deviceMapper, recordMapper, redissonClient);
    }

    @Override
    AirConditionRecord decryptPayload(byte[] payload, Long rs485Id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decryptPayload'");
    }

    @Override
    AirCondition parse(byte[] payload, Long rs485Id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'parse'");
    }

    @Override
    boolean needSave(byte[] payload) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'needSave'");
    }

    @Override
    boolean verify(byte[] payload) {
        return SumChecker.verifyCheckSum(payload);
    }

}
