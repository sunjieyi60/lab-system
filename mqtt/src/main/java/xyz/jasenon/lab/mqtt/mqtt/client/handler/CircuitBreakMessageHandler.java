package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.CircuitBreak;
import xyz.jasenon.lab.common.entity.record.CircuitBreakRecord;
import xyz.jasenon.lab.common.utils.CrcChecker;
import xyz.jasenon.lab.mqtt.mapper.CircuitBreakMapper;
import xyz.jasenon.lab.mqtt.mapper.CircuitBreakRecordMapper;

import java.text.MessageFormat;
import java.util.Stack;

@Component
@Slf4j
public class CircuitBreakMessageHandler
        extends MqttMessageHandler<CircuitBreakMapper, CircuitBreakRecordMapper, CircuitBreak, CircuitBreakRecord> {

    public CircuitBreakMessageHandler(CircuitBreakMapper deviceMapper, CircuitBreakRecordMapper recordMapper,
            RedissonClient redissonClient) {
        super(deviceMapper, recordMapper, redissonClient);
    }

    @Override
    CircuitBreakRecord decryptPayload(byte[] payload, Long rs485Id) {
        Integer address = payload[0] & 0xFF;
        byte[] payload1 = new byte[] { payload[3], payload[4] };
        String fixStatus = Integer.toBinaryString(payload1[0]).length() > 1 ? Integer.toBinaryString(payload1[0])
                : "0" + Integer.toBinaryString(payload1[0]);
        String openLockStatus = Integer.toBinaryString(payload1[1]).length() > 1 ? Integer.toBinaryString(payload1[1])
                : "0" + Integer.toBinaryString(payload1[1]);

        boolean isFix = fixStatus.toCharArray()[1] == '1';
        boolean isOpen = openLockStatus.toCharArray()[1] == '1';
        boolean isLock = openLockStatus.toCharArray()[0] == '1';

        byte[] payload2 = new byte[] { payload[7], payload[8], payload[9], payload[10] };
        float leakage = reserverByteThenToFloat(payload2);
        byte[] payload3 = new byte[] { payload[11], payload[12], payload[13], payload[14] };
        float temperture = reserverByteThenToFloat(payload3);
        byte[] payload4 = new byte[] { payload[55], payload[56], payload[57], payload[58] };
        float voltage = reserverByteThenToFloat(payload4);
        byte[] payload5 = new byte[] { payload[119], payload[120], payload[121], payload[122] };
        float current = reserverByteThenToFloat(payload5);
        byte[] payload6 = new byte[] { payload[151], payload[152], payload[153], payload[154] };
        float power = reserverByteThenToFloat(payload6);
        byte[] payload7 = new byte[] { payload[215], payload[216], payload[217], payload[218] };
        float energy = reserverByteThenToFloat(payload7);
        log.info("电表地址:{}", payload[0]);
        log.info("漏电值:{}", leakage);
        log.info("温度值:{}", temperture);
        log.info("电压值:{}", voltage);
        log.info("电流值:{}", current);
        log.info("功率值:{}", power);
        log.info("电能值:{}", energy);
        log.info("合闸:{}", isFix);
        log.info("门锁:{}", isLock);
        log.info("分闸:{}", isOpen);

        CircuitBreakRecord circuitBreakRecord = (CircuitBreakRecord) new CircuitBreakRecord()
                .address(address)
                .current(current)
                .energy(energy)
                .isFix(isFix)
                .isLock(isLock)
                .isOpen(isOpen)
                .leakage(leakage)
                .power(power)
                .temperature(temperture)
                .voltage(voltage)
                .rs485Id(rs485Id);

        return circuitBreakRecord;
    }

    @Override
    CircuitBreak parse(byte[] payload, Long rs485Id) {
        Integer address = payload[0] & 0xff;

        CircuitBreak circuitBreak = new LambdaQueryChainWrapper<>(super.deviceMapper)
                .eq(CircuitBreak::getAddress, address)
                .eq(CircuitBreak::getRs485GatewayId, rs485Id)
                .one();

        return circuitBreak;
    }

    @Override
    boolean needSave(byte[] payload) {
        if (payload[1] == 16) {
            log.info("CRC OK");
            log.info("地址:{}", payload[0]);
            log.info("用户操作成功");
            return false;
        }
        if (payload[1] == 3) {
            log.info("CRC OK");
            log.info("地址:{}", payload[0]);
            log.info("polling成功");
            return true;
        }
        throw new IllegalArgumentException(MessageFormat.format("未知的指令{0}", payload[1]));
    }

    @Override
    boolean verify(byte[] payload) {
        return CrcChecker.varify(payload);
    }

    public static float reserverByteThenToFloat(byte[] bytes) {
        Stack<Byte> reserver = new Stack<>();
        for (byte b : bytes) {
            reserver.push(b);
        }
        StringBuilder hexString = new StringBuilder();
        while (!reserver.isEmpty()) {
            byte b = reserver.pop();
            hexString.append(Integer.toHexString(b));
        }
        int hexVal = Integer.parseInt(hexString.toString(), 16);
        return Float.intBitsToFloat(hexVal);
    }

}
