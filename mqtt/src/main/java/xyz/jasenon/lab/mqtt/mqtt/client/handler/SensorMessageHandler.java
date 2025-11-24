package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import java.text.MessageFormat;

import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;

import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.common.entity.device.Sensor;
import xyz.jasenon.lab.common.entity.record.SensorRecord;
import xyz.jasenon.lab.common.utils.SumChecker;
import xyz.jasenon.lab.mqtt.mapper.SensorMapper;
import xyz.jasenon.lab.mqtt.mapper.SensorRecordMapper;

@Component
@Slf4j
public class SensorMessageHandler extends MqttMessageHandler<SensorMapper, SensorRecordMapper, Sensor, SensorRecord> {

    public SensorMessageHandler(SensorMapper deviceMapper, SensorRecordMapper recordMapper,
            RedissonClient redissonClient) {
        super(deviceMapper, recordMapper, redissonClient);
    }

    @Override
    SensorRecord decryptPayload(byte[] payload, Long rs485Id) {
        Integer address = payload[0] & 0xff;
        Integer selfId = payload[2] & 0xff;
        Double temperature = Integer.parseInt(Integer.toHexString(payload[3]) + Integer.toHexString(payload[4]), 16)
                / 10.0;
        Double humidity = Integer.parseInt(Integer.toHexString(payload[5]) + Integer.toHexString(payload[6]), 16)
                / 10.0;
        Double light = Integer.parseInt(Integer.toHexString(payload[7]) + Integer.toHexString(payload[8])
                + Integer.toHexString(payload[9]) + Integer.toHexString(payload[10]), 16) / 10.0;
        Integer smoke = Integer.parseInt(Integer.toHexString(payload[11]) + Integer.toHexString(payload[12]), 16);

        SensorRecord sensorRecord = (SensorRecord) new SensorRecord()
                .temperature(temperature)
                .humidity(humidity)
                .light(light)
                .smoke(smoke)
                .address(address)
                .selfId(selfId)
                .rs485Id(rs485Id);

        return sensorRecord;
    }

    @Override
    Sensor parse(byte[] payload, Long rs485Id) {
        Integer address = payload[0] & 0xff;
        Integer selfId = payload[2] & 0xff;

        Sensor sensor = new LambdaQueryChainWrapper<>(super.deviceMapper)
                .eq(Sensor::getSelfId, selfId)
                .eq(Sensor::getAddress, address)
                .eq(Sensor::getRs485GatewayId, rs485Id)
                .one();

        return sensor;
    }

    @Override
    boolean needSave(byte[] payload) {
        if (payload[1] == 0x0A) {
            log.info("地址:{0}", payload[0]);
            log.info("用户操作成功");
            return false;
        }
        if (payload[1] == 0x03) {
            log.info("地址:{0}", payload[0]);
            log.info("polling成功");
            return true;
        }
        return false;
    }

    @Override
    boolean verify(byte[] payload) {
        return SumChecker.verifyCheckSum(payload);
    }

}
