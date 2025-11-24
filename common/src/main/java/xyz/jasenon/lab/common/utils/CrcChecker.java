package xyz.jasenon.lab.common.utils;

public class CrcChecker {

    public static int calculateCRC16(byte[] data, int length) {
        int CRC16 = 0xFFFF;
        for (int i = 0; i < length; i++) {
            CRC16 ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((CRC16 & 0x0001) != 0) {
                    CRC16 = (CRC16 >> 1) ^ 0xA001;
                } else {
                    CRC16 >>= 1;
                }
            }
        }
        return CRC16 & 0xFFFF;
    }

    public static boolean varify(byte[] bytes) {
        if (bytes.length < 2) {
            throw new IllegalArgumentException("数据非法");
        }
        // 传递数据长度，排除校验位
        int crc16 = calculateCRC16(bytes, bytes.length - 2);
        String hexString = Integer.toHexString(bytes[bytes.length - 1]) + Integer.toHexString(bytes[bytes.length - 2]);
        int crc16Hex = Integer.parseInt(hexString, 16);
        return crc16 == crc16Hex;
    }

    public static byte[] generatePayload(Byte[] originalPayload){
        byte[] payload = new byte[originalPayload.length + 2];
        for (int i = 0; i < originalPayload.length; i++) {
            payload[i] = originalPayload[i].byteValue();
        }
        int crc16 = calculateCRC16(payload, originalPayload.length);
        // 校验位在前，数据在后
        payload[originalPayload.length] = (byte) ((crc16 >> 8) & 0xFF);
        payload[originalPayload.length + 1] = (byte) (crc16 & 0xFF);
        return payload;
    }
}
