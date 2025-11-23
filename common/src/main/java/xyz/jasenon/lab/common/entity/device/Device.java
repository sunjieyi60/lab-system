package xyz.jasenon.lab.common.entity.device;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Getter
@Setter
@Accessors(chain = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "deviceType", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AirCondition.class, name = "AirCondition"),
    @JsonSubTypes.Type(value = Light.class, name = "Light"),
    @JsonSubTypes.Type(value = Access.class, name = "Access"),
    @JsonSubTypes.Type(value = Sensor.class, name = "Sensor"),
    @JsonSubTypes.Type(value = CircuitBreak.class, name = "CircuitBreak"),
})
public class Device extends BaseEntity {
    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型（必须与子类枚举值完全匹配）
     */
    protected DeviceType deviceType;

    /**
     * 设备所属实验室ID
     */
    private Long belongToLaboratoryId;

}
