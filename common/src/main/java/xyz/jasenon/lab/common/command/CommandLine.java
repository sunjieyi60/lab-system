package xyz.jasenon.lab.common.command;

import lombok.Getter;

@Getter
public enum CommandLine {

    OPEN_AIR_CONDITION_RS485(new Command("{0} {1} 01 FF FF FF FF FF FF",CheckType.SUM_SG,SendType.MQTT),"打开空调"),
    CLOSE_AIR_CONDITION_RS485(new Command("{0} {1} 00 FF FF FF FF FF FF",CheckType.SUM_SG,SendType.MQTT),"关闭空调"),
    ENHANCE_CONTROL_AIR_CONDITION(new Command("{0} {1} {2} {3} {4} {5} FF FF FF",CheckType.SUM_SG,SendType.MQTT),"增强控制空调"),
    REQUEST_AIR_CONDITION_DATA_RS485(new Command("{0} {1} FF FF FF FF FF FF FF",CheckType.SUM_SG,SendType.MQTT),"请求空调数据"),
    REQUEST_AIR_CONDITION_DATA_SOCKET(new Command("{0} 88 00 00",CheckType.SUM_SG,SendType.SOCKET),"请求空调数据"),



    OPEN_ACCESS_ONCE(new Command("{0} 0B {1} 00 00 00",CheckType.SUM_SG,SendType.MQTT),"单次开门"),
    CLOSE_ACCESS_ONCE(new Command("{0} 0A {1} 00 FF 00",CheckType.SUM_SG,SendType.MQTT),"单次关门"),
    REQUEST_ACCESS_DATA(new Command("{0} 03 {1} 00 00 00",CheckType.SUM_SG,SendType.MQTT), "请求门禁数据"),
    OPEN_ACCESS_PERSIST_LOCK(new Command("{0} 0A {1} FF FF 00",CheckType.SUM_SG,SendType.MQTT),"长开锁定"),
    OPEN_ACCESS_PERSIST_UNLOCK(new Command("{0} 0A {1} FF 00 00",CheckType.SUM_SG,SendType.MQTT),"长开解锁"),
    OPEN_ACCESS_PERSIST_KEEP(new Command("{0} 0A {1} FF 11 00",CheckType.SUM_SG,SendType.MQTT),"长开保持原状"),
    CLOSE_ACCESS_PERSIST_LOCK(new Command("{0} 0A {1} 00 FF 00",CheckType.SUM_SG,SendType.MQTT),"长关锁定"),
    CLOSE_ACCESS_PERSIST_UNLOCK(new Command("{0} 0A {1} 00 00 00",CheckType.SUM_SG,SendType.MQTT),"长关解锁"),
    CLOSE_ACCESS_PERSIST_KEEP(new Command("{0} 0A {1} 00 11 00",CheckType.SUM_SG,SendType.MQTT),"长关保持原状"),
    KEEP_ACCESS_STATUS_LOCK(new Command("{0} 0A {1} 11 FF 00",CheckType.SUM_SG,SendType.MQTT),"保持门禁锁定状态"),
    KEEP_ACCESS_STATUS_UNLOCK(new Command("{0} 0A {1} 11 00 00",CheckType.SUM_SG,SendType.MQTT),"保持门禁解锁状态"),
    SET_ACCESS_DELAY(new Command("{0} 0C {1} {2} 00 00",CheckType.SUM_SG,SendType.MQTT),"延时设定门禁"),
    OPEN_CIRCUITBREAK(new Command("{0} 10 00 19 00 01 02 00 01",CheckType.CRC16,SendType.MQTT),"断路器合闸"),
    CLOSE_CIRCUITBREAK(new Command("{0} 10 00 19 00 01 02 00 00",CheckType.CRC16,SendType.MQTT),"断路器分闸"),
    REQUEST_CIRCUITBREAK_DATA(new Command("{0} 03 00 18 00 74",CheckType.CRC16,SendType.MQTT),"请求断路器数据"),

    OPEN_LIGHT(new Command("{0} 0A {1} FF 11 00",CheckType.SUM_UNSG,SendType.MQTT),"打开灯光"),
    CLOSE_LIGHT(new Command("{0} 0A {1} 00 11 00",CheckType.SUM_UNSG,SendType.MQTT),"关闭灯光"),
    REQUEST_LIGHT_DATA(new Command("{0} 03 {1} 00 00 00",CheckType.SUM_UNSG,SendType.MQTT), "请求灯光数据"),

    REQUEST_SENSOR_DATA(new Command("{0} 03 {1} 00 00",CheckType.SUM_UNSG,SendType.MQTT),"请求传感器数据"),;


    private final Command command;
    private final String description;
    CommandLine(Command command, String description){
        this.command = command;
        this.description = description;
    }

    /**
     * 根据指令与 args 生成参数级描述文案（用于操作日志等）。
     * 仅对需要参数描述的指令返回非空；其余返回 null，调用方保持原样即可。
     * args 下标与枚举值与协议约定一致（如空调增强控制：args[2]=开关, args[3]=模式, args[4]=温度, args[5]=风速）。
     */
    public String paramDetail(Integer[] args) {
        if (args == null) {
            return null;
        }
        return switch (this) {
            case ENHANCE_CONTROL_AIR_CONDITION -> buildAirConditionEnhanceParamDetail(args);
            default -> null;
        };
    }

    /**
     * 空调增强控制：args[0]=address, args[1]=selfId, args[2]=开关, args[3]=模式, args[4]=温度, args[5]=风速。
     * 协议枚举：开关 0=关 1=开；模式 1=制热 2=制冷 4=送风 8=除湿；风速 0=自动 1=低 2=中 3=高。
     */
    private static String buildAirConditionEnhanceParamDetail(Integer[] args) {
        if (args.length < 6) {
            return null;
        }
        String switchStr = formatSwitch(args[2]);
        String modeStr = formatMode(args[3]);
        String tempStr = formatTemperature(args[4]);
        String speedStr = formatSpeed(args[5]);
        return "、" + switchStr + "、" + modeStr + "、" + tempStr + "、" + speedStr;
    }

    private static String formatSwitch(Integer v) {
        if (v == null) return "-";
        return switch (v) {
            case 0 -> "关";
            case 1 -> "开机";
            default -> String.valueOf(v);
        };
    }

    private static String formatMode(Integer v) {
        if (v == null) return "-";
        return switch (v) {
            case 1 -> "制热";
            case 2 -> "制冷";
            case 4 -> "送风";
            case 8 -> "除湿";
            default -> String.valueOf(v);
        };
    }

    private static String formatTemperature(Integer v) {
        if (v == null) return "-";
        return v + "℃";
    }

    private static String formatSpeed(Integer v) {
        if (v == null) return "-";
        return switch (v) {
            case 0 -> "自动";
            case 1 -> "低风";
            case 2 -> "中风";
            case 3 -> "高风";
            default -> String.valueOf(v);
        };
    }

}
