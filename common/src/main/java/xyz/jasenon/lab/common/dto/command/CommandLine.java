package xyz.jasenon.lab.common.dto.command;

import lombok.Getter;

@Getter
public enum CommandLine {

    OPEN_AIR_CONDITION_RS485(new Command("{0} {1} 01 FF FF FF FF FF FF",CheckType.SUM,SendType.MQTT),"打开空调"),
    CLOSE_AIR_CONDITION_RS485(new Command("{0} {1} 00 FF FF FF FF FF FF",CheckType.SUM,SendType.MQTT),"关闭空调"),
    ENHANCE_CONTROL_AIR_CONDITION(new Command("{0} {1} {2} {3} {4} {5} FF FF FF",CheckType.SUM,SendType.MQTT),"增强控制空调"),
    RQEUEST_AIR_CONDITION_DATA_RS485(new Command("{0} {1} FF FF FF FF FF FF FF",CheckType.SUM,SendType.MQTT),"请求空调数据"),
    REQUEST_AIR_CONDITION_DATA_SOCKET(new Command("{0} 88 00 00",CheckType.SUM,SendType.SOCKET),"请求空调数据"),


    OPEN_ACCESS_ONCE(new Command("{0} 0B {1} 00 00 00",CheckType.SUM,SendType.MQTT),"单次开门"),

    OPEN_CIRCUITBREAK(new Command("{0} 10 00 19 00 01 02 00 01",CheckType.CRC16,SendType.MQTT),"断路器分闸"),
    CLOSE_CIRCUITBREAK(new Command("{0} 10 00 19 00 01 02 00 01",CheckType.CRC16,SendType.MQTT),"断路器合闸"),

    OPEN_LIGHT(new Command("{0} 0A {1} FF 11 00",CheckType.SUM,SendType.MQTT),"打开灯光"),
    CLOSE_LIGHT(new Command("{0} 0A {1} 00 11 00",CheckType.SUM,SendType.MQTT),"关闭灯光"),

    REQUEST_SENSOR_DATA(new Command("{0} 03 {1} 00 00",CheckType.SUM,SendType.MQTT),"请求传感器数据"),;


    private final Command command;
    private final String description;
    CommandLine(Command command, String description){
        this.command = command;
        this.description = description;
    }

}
