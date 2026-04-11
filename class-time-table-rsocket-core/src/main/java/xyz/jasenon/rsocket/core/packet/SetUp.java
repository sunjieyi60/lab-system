package xyz.jasenon.rsocket.core.packet;

import com.alibaba.fastjson2.JSON;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Unique;

/**
 * <p>可以为setup扩展校验机制</p>
 */
@Getter
@Setter
public class SetUp implements Unique {
    /**
     * 设备uuid
     */
    private String uuid;

    @Override
    public String unique() {
        return uuid;
    }

    @Override
    public String toString(){
        return JSON.toJSONString(this);
    }
}
