package xyz.jasenon.lab.common.entity.device;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class DeviceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDeserializeAirCondition() throws Exception {
        String json = "{\"deviceName\":\"空调1号\",\"deviceType\":\"AirCondition\",\"belongToLaboratoryId\":1}";
        Device device = objectMapper.readValue(json, Device.class);
        log.info("object:{},object.class:{}",device,device.getClass());
        assertTrue(device instanceof AirCondition);
        assertEquals("空调1号", device.getDeviceName());
    }

    @Test
    void testDeserializeLight() throws Exception {
        String json = "{\"deviceName\":\"LED灯\",\"deviceType\":\"Light\",\"belongToLaboratoryId\":2}";
        Device device = objectMapper.readValue(json, Device.class);
        log.info("object:{},object.class:{}",device,device.getClass());
        assertTrue(device instanceof Light);
        assertEquals("LED灯", device.getDeviceName());
    }
}