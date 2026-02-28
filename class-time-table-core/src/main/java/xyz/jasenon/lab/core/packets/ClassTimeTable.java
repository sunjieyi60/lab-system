package xyz.jasenon.lab.core.packets;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Jasenon_ce
 * @date 2026/2/28
 */
@Getter
@Setter
public class ClassTimeTable extends Message implements Serializable {

    /**
     * 设备uuid
     */
    private String uuid;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备状态
     */
    private String status = ClassTimeTableStatusType.OFFLINE.getStatus();

}
