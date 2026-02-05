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
    CircuitBreakRecord decryptPayload(byte[] payload, Long deviceId) {
        Integer address = payload[0] & 0xFF;
        byte[] payload1 = new byte[] { payload[3], payload[4] };
        String fixStatus = Integer.toBinaryString(payload1[0]).length() > 1 ? Integer.toBinaryString(payload1[0])
                : "0" + Integer.toBinaryString(payload1[0]);
        String openLockStatus = Integer.toBinaryString(payload1[1]).length() > 1 ? Integer.toBinaryString(payload1[1])
                : "0" + Integer.toBinaryString(payload1[1]);

        boolean isFix = fixStatus.toCharArray()[1] == '1';
        boolean isOpen = openLockStatus.toCharArray()[1] == '1';
        boolean isLock = openLockStatus.toCharArray()[0] == '1';

        byte[] payload2 = new byte[] {(byte) (payload[7] & 0xff), (byte) (payload[8] & 0xff), (byte) (payload[9] & 0xff),(byte) (payload[10] & 0xff) };
        float leakage = reserverByteThenToFloat(payload2);
        byte[] payload3 = new byte[] { (byte) (payload[11] & 0xff),(byte) (payload[12] & 0xff),(byte) (payload[13] & 0xff),(byte) (payload[14] & 0xff) };
        float temperture = reserverByteThenToFloat(payload3);
        byte[] payload4 = new byte[] { (byte) (payload[55] & 0xff),(byte) (payload[56] & 0xff), (byte) (payload[57] & 0xff),(byte) (payload[58] & 0xff) };
        float voltage = reserverByteThenToFloat(payload4);
        byte[] payload5 = new byte[] { (byte) (payload[119] & 0xff), (byte) (payload[120] & 0xff), (byte) (payload[121] & 0xff),(byte) (payload[122] & 0xff) };
        float current = reserverByteThenToFloat(payload5);
        byte[] payload6 = new byte[] { (byte) (payload[151] & 0xff), (byte) (payload[152] & 0xff), (byte) (payload[153] & 0xff), (byte) (payload[154] & 0xff) };
        float power = reserverByteThenToFloat(payload6);
        byte[] payload7 = new byte[] { (byte) (payload[215] & 0xff), (byte) (payload[216] & 0xff), (byte) (payload[217] & 0xff), (byte) (payload[218] & 0xff) };
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
                .setAddress(address)
                .setCurrent(current)
                .setEnergy(energy)
                .setIsFix(isFix)
                .setIsLock(isLock)
                .setIsOpen(isOpen)
                .setLeakage(leakage)
                .setPower(power)
                .setTemperature(temperture)
                .setVoltage(voltage)
                .setDeviceId(deviceId);

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
            Integer b = reserver.pop() & 0xff;
            hexString.append(Integer.toHexString(b).length() > 1? Integer.toHexString(b): "0" + Integer.toHexString(b));
        }
        int hexVal = Integer.parseInt(hexString.toString(), 16);
        return Float.intBitsToFloat(hexVal);
    }

}
