package xyz.jasenon.rsocket.core.protocol;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum Command {

    REGISTER(1,"设备注册");

    private final Integer code;

    private final String desc;

    Command(Integer code, String desc){
        this.code = code;
        this.desc = desc;
    }

    private static Map<Integer, Command> CACHE = Arrays.stream(values())
            .collect(Collectors.toMap(Command::getCode, cmd -> cmd));

    public static Command valueOf(Integer code){
        if (code == null){
            return null;
        }
        return CACHE.getOrDefault(code,null);
    }
}
