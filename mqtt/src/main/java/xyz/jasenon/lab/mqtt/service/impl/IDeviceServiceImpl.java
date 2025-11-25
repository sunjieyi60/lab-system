package xyz.jasenon.lab.mqtt.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import xyz.jasenon.lab.common.entity.device.Access;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.mqtt.mapper.DeviceMapper;
import xyz.jasenon.lab.mqtt.mapper.AccessMapper;
import xyz.jasenon.lab.mqtt.mapper.LightMapper;
import xyz.jasenon.lab.mqtt.mapper.AirConditionMapper;
import xyz.jasenon.lab.mqtt.mapper.SensorMapper;
import xyz.jasenon.lab.mqtt.mapper.CircuitBreakMapper;
import xyz.jasenon.lab.mqtt.service.IDeviceService;

@Service
public class IDeviceServiceImpl extends ServiceImpl<DeviceMapper,Device> implements IDeviceService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public Device getDeviceByDeviceId(Long id) {
        return deviceMapper.selectById(id);
    }



}
