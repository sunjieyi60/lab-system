package xyz.jasenon.lab.service.strategy.device;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.device.*;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import xyz.jasenon.lab.service.strategy.device.ex.*;

import java.util.List;

@Component
@Slf4j
public class DevicePollingStarter {

    @Autowired
    private ILaboratoryService laboratoryService;
    @Autowired
    private AccessQ accessCreateStrategy;
    @Autowired
    private AirConditionQ airConditionCreateStrategy;
    @Autowired
    private CircuitBreakQ circuitBreakCreateStrategy;
    @Autowired
    private SensorQ sensorCreateStrategy;
    @Autowired
    private LightQ lightCreateStrategy;

    @PostConstruct
    public void init(){
        log.info("轮询注册器启动");
        List<Long> laboratoryIds = laboratoryService.list().stream().map(Laboratory::getId).toList();
        List<Access> accesses = accessCreateStrategy.list(laboratoryIds);
        List<AirCondition> airConditions = airConditionCreateStrategy.list(laboratoryIds);
        List<CircuitBreak> circuitBreaks = circuitBreakCreateStrategy.list(laboratoryIds);
        List<Light> lights = lightCreateStrategy.list(laboratoryIds);
        List<Sensor> sensors = sensorCreateStrategy.list(laboratoryIds);
        accesses.forEach(
            access -> accessCreateStrategy.startPolling(access)
        );
        airConditions.forEach(
            airCondition -> airConditionCreateStrategy.startPolling(airCondition)
        );
        circuitBreaks.forEach(
            circuitBreak -> circuitBreakCreateStrategy.startPolling(circuitBreak)
        );
        lights.forEach(
            light -> lightCreateStrategy.startPolling(light)
        );
        sensors.forEach(
            sensor -> sensorCreateStrategy.startPolling(sensor)
        );
        log.info("轮询注册器完成注册");
    }

}
