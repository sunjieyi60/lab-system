package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import java.text.MessageFormat;

import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;

import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.common.entity.device.Light;
import xyz.jasenon.lab.common.entity.record.LightRecord;
import xyz.jasenon.lab.common.utils.SumChecker;
import xyz.jasenon.lab.mqtt.mapper.LightMapper;
import xyz.jasenon.lab.mqtt.mapper.LightRecordMapper;

@Component
@Slf4j
public class LightMessageHandler extends MqttMessageHandler<LightMapper, LightRecordMapper, Light, LightRecord> {

    public LightMessageHandler(LightMapper deviceMapper, LightRecordMapper recordMapper,
            RedissonClient redissonClient) {
        super(deviceMapper, recordMapper, redissonClient);
    }

    @Override
    LightRecord decryptPayload(byte[] payload, Long rs485Id) {
        Integer address = payload[0] & 0xFF;
        Integer selfId = payload[1] & 0xFF;

        LightRecord lightRecord = (LightRecord) new LightRecord()
                .address(address)
                .selfId(selfId)
                .isOpen(payload[3] == (byte) 0xff)
                .isLock(payload[4] == (byte) 0xff)
                .rs485Id(rs485Id);

        return lightRecord;
    }

    @Override
    Light parse(byte[] payload, Long rs485Id) {
        Integer address = payload[0] & 0xFF;
        Integer selfId = payload[1] & 0xFF;

        Light light = new LambdaQueryChainWrapper<>(super.deviceMapper)
                .eq(Light::getAddress, address)
                .eq(Light::getSelfId, selfId)
                .eq(Light::getRs485GatewayId, selfId)
                .one();

        return light;
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
        throw new IllegalArgumentException(MessageFormat.format("未知的指令{0}", payload[1]));
    }

    @Override
    boolean verify(byte[] payload) {
        return SumChecker.verifyCheckSum(payload);
    }

}
