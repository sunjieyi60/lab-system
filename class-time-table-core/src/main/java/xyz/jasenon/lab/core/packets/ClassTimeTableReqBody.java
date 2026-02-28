package xyz.jasenon.lab.core.packets;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2026/2/28
 */
@Getter
@Setter
public class ClassTimeTableReqBody extends Message{

    /**
     * 设备id
     */
    private String uuid;

    /**
     * 0:单个,1:所有在线班牌,2:所有在线离线班牌
     */
    private Integer type;

}
