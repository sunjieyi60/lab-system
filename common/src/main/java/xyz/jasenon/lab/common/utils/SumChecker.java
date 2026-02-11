package xyz.jasenon.lab.common.utils;

public class SumChecker {

    public static byte calculateCheckSum(byte[] data) {
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return (byte) (sum % 0xFF);
    }

    public static boolean verifyCheckSum(byte[] data){
        // 计算除最后一位外的所有字节的和
        int sum = 0;
        for (int i = 0; i < data.length - 1; i++) {
            sum += data[i];  // 使用 & 0xFF 确保使用无符号字节值
        }

        byte checksum = (byte) (sum  % 0xFF);
        return checksum == data[data.length - 1];
    }

    public static byte calculateUnsignedByteCheckSum(byte[] data){
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i] & 0xff;  // 使用 & 0xFF 确保使用无符号字节值
        }
        return (byte) (sum % 0xFF);
    }

    public static boolean verifyUnsignedByteCheckSum(byte[] data){
        // 计算除最后一位外的所有字节的和
        int sum = 0;
        for (int i = 0; i < data.length - 1; i++) {
            sum += data[i] & 0xff;  // 使用 & 0xFF 确保使用无符号字节值
        }

        byte checksum = (byte) (sum % 0xFF);
        return checksum == data[data.length - 1];
    }

    public static byte[] generatePayload(Byte[] originalPayload) {
        byte[] payload = new byte[originalPayload.length + 1];  // 加一个校验位
        for (int i = 0; i < originalPayload.length; i++) {
            payload[i] = originalPayload[i];
        }
        payload[originalPayload.length] = calculateCheckSum(payload); // 计算校验和并添加到数组末尾
        return payload;
    }

    public static byte[] generateUnsignedBytePayload(Byte[] originalPayload) {
        byte[] payload = new byte[originalPayload.length + 1];  // 加一个校验位
        for (int i = 0; i < originalPayload.length; i++) {
            payload[i] = originalPayload[i];
        }
        payload[originalPayload.length] = calculateUnsignedByteCheckSum(payload); // 计算校验和并添加到数组末尾
        return payload;
    }
}
