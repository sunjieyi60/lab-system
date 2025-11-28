package xyz.jasenon.lab.service.dto.device;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.device.DeviceType;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "deviceType", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreateAirCondition.class, name = "AirCondition"),
    @JsonSubTypes.Type(value = CreateLight.class, name = "Light"),
    @JsonSubTypes.Type(value = CreateAccess.class, name = "Access"),
    @JsonSubTypes.Type(value = CreateSensor.class, name = "Sensor"),
    @JsonSubTypes.Type(value = CreateCircuitBreak.class, name = "CircuitBreak")
})
public class CreateDevice {

    @NotNull(message = "设备名称不能为空")
    private String deviceName;

    @NotNull(message = "设备类型不能为空")
    private DeviceType deviceType;

    @NotNull(message = "所属实验室不能为空")
    private Long belongToLaboratoryId;

}
