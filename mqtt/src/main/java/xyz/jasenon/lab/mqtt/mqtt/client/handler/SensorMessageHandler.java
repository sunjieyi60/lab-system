package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.Sensor;
import xyz.jasenon.lab.common.entity.record.SensorRecord;
import xyz.jasenon.lab.common.utils.SumChecker;
import xyz.jasenon.lab.mqtt.mapper.SensorMapper;
import xyz.jasenon.lab.mqtt.mapper.SensorRecordMapper;

import java.text.MessageFormat;

@Component
@Slf4j
public class SensorMessageHandler extends MqttMessageHandler<SensorMapper, SensorRecordMapper, Sensor, SensorRecord> {

    public SensorMessageHandler(SensorMapper deviceMapper, SensorRecordMapper recordMapper,
            RedissonClient redissonClient) {
        super(deviceMapper, recordMapper, redissonClient);
    }

    @Override
    SensorRecord decryptPayload(byte[] payload, Long deviceId) {
        Integer address = payload[0] & 0xff;
        Integer selfId = payload[2] & 0xff;
        String hex = String.format("%02x", payload[3]) + String.format("%02x", payload[4]);
        String hex1 = String.format("%02x", payload[3] & 0xff) + String.format("%02x", payload[4] & 0xff);
        Double temperature = Integer.parseInt(String.format("%02x", payload[3] & 0xff) + String.format("%02x", payload[4] & 0xff), 16)
                / 10.0;
        Double humidity = Integer.parseInt(String.format("%02x", payload[5] & 0xff) + String.format("%02x", payload[6] & 0xff), 16)
                / 10.0;
        Double light = Integer.parseInt(String.format("%02x", payload[7] & 0xff) + String.format("%02x", payload[8] & 0xff)
                + String.format("%02x", payload[9] & 0xff) + String.format("%02x", payload[10] & 0xff), 16) / 10.0;
        Integer smoke = Integer.parseInt(String.format("%02x", payload[11] & 0xff) + String.format("%02x", payload[12] & 0xff), 16);

        SensorRecord sensorRecord = (SensorRecord) new SensorRecord()
                .setTemperature(temperature)
                .setHumidity(humidity)
                .setLight(light)
                .setSmoke(smoke)
                .setAddress(address)
                .setSelfId(selfId)
                .setDeviceId(deviceId);

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
        return SumChecker.verifyUnsignedByteCheckSum(payload);
    }

}
