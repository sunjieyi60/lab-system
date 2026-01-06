package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.base.LaboratoryUser;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;
import xyz.jasenon.lab.common.entity.device.gateway.SocketGateway;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;
import xyz.jasenon.lab.service.mapper.record.DeviceMapper;
import xyz.jasenon.lab.service.mapper.LaboratoryMapper;
import xyz.jasenon.lab.service.mapper.record.RS485GatewayMapper;
import xyz.jasenon.lab.service.mapper.record.SocketGatewayMapper;
import xyz.jasenon.lab.service.service.IDeviceService;
import xyz.jasenon.lab.service.strategy.device.DeviceFactory;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordFactory;
import xyz.jasenon.lab.service.vo.DeviceRecordVo;
import xyz.jasenon.lab.service.vo.DeviceVo;
import xyz.jasenon.lab.service.vo.Rs485GatewayVo;
import xyz.jasenon.lab.service.vo.SocketGatewayVo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService {

    @Autowired
    private LaboratoryMapper laboratoryMapper;
    @Autowired
    private RS485GatewayMapper rs485GatewayMapper;
    @Autowired
    private SocketGatewayMapper socketGatewayMapper;


    @Override
    public R deleteDevice(DeleteDevice deleteDevice) {
        baseMapper.deleteById(deleteDevice.getDeviceId());
        return R.success("删除成功");
    }

    @Override
    public R<Map<Long, List<Rs485GatewayVo>>> getRs485GatewayTree() {
        List<Laboratory> laboratoryList = getVisibleLaboratories();
        List<RS485Gateway> rs485GatewayList = new ArrayList<>();
        for (Laboratory laboratory : laboratoryList) {
            List<RS485Gateway> part = rs485GatewayMapper.selectList(
                    new LambdaQueryWrapper<RS485Gateway>()
                            .eq(RS485Gateway::getBelongToLaboratoryId, laboratory.getId())
            );
            rs485GatewayList.addAll(part);
        }
        Map<Long, List<Rs485GatewayVo>> map = rs485GatewayList.stream().map(rs485Gateway -> {
            Rs485GatewayVo vo = new Rs485GatewayVo();
            vo.setGatewayId(rs485Gateway.getId());
            vo.setGatewayName(rs485Gateway.getGatewayName());
            vo.setLaboratoryId(rs485Gateway.getBelongToLaboratoryId());
            vo.setSendTopic(rs485Gateway.getSendTopic());
            vo.setAcceptTopic(rs485Gateway.getAcceptTopic());
            return vo;
        }).collect(Collectors.groupingBy(Rs485GatewayVo::getLaboratoryId, Collectors.toList()));
        return R.success(map,"获取成功");
    }

    @Override
    public R<Map<Long, List<SocketGatewayVo>>> getSocketGatewayTree() {
        List<Laboratory> laboratoryList = getVisibleLaboratories();
        List<SocketGateway> socketGatewayList = new ArrayList<>();
        for (Laboratory laboratory : laboratoryList) {
            List<SocketGateway> part = socketGatewayMapper.selectList(
                    new LambdaQueryWrapper<SocketGateway>()
                            .eq(SocketGateway::getBelongToLaboratoryId, laboratory.getId())
            );
            socketGatewayList.addAll(part);
        }
        Map<Long, List<SocketGatewayVo>> map = socketGatewayList.stream().map(socketGateway -> {
            SocketGatewayVo vo = new SocketGatewayVo();
            vo.setGatewayId(socketGateway.getId());
            vo.setGatewayName(socketGateway.getGatewayName());
            vo.setIp(socketGateway.getIp());
            vo.setMac(socketGateway.getMac());
            vo.setLaboratoryId(socketGateway.getBelongToLaboratoryId());
            return vo;
        }).collect(Collectors.groupingBy(SocketGatewayVo::getGatewayId, Collectors.toList()));
        return R.success(map,"获取成功");
    }

    private List<Laboratory> getVisibleLaboratories() {
        Long doUserId = StpUtil.getLoginIdAsLong();
        List<Laboratory> laboratoryList = laboratoryMapper.selectJoinList(
                new MPJLambdaWrapper<Laboratory>()
                        .selectAll(Laboratory.class)
                        .leftJoin(LaboratoryUser.class, on ->
                                on.eq(LaboratoryUser::getUserId, doUserId)
                                        .eq(LaboratoryUser::getLaboratoryId, Laboratory::getId)
                        )
        );
        return laboratoryList;
    }

    @Override
    public R<Map<Long,List<DeviceVo>>> listDevice(List<Long> laboratoryIds, DeviceType deviceType) {
        List<Long> laboratoryIdsVisible = getVisibleLaboratories().stream().map(l->l.getId()).toList();
        boolean isOver = !new HashSet<>(laboratoryIdsVisible).containsAll(laboratoryIds);
        if(isOver){
            return R.fail("查询越权!");
        }
        List<? extends Device> res = DeviceFactory.getDeviceQMethod(deviceType).list(laboratoryIds);
        if(res.isEmpty()){
            return R.fail("查询无结果");
        }
        List<DeviceVo> devices = res.stream().filter(r->r.getId()!=null).map(device -> {
            DeviceVo vo = new DeviceVo();
            DeviceRecordVo<?> record = DeviceRecordFactory.getDeviceRecordMethod(deviceType).getRecord(device.getId());
            vo.setDeviceRecord(record);
            vo.setDevice(device);
            return vo;
        }).toList();
        Map<Long,List<DeviceVo>> map = devices.stream()
                .filter(vo->vo.getDevice()!=null)
                .collect(Collectors.groupingBy(DeviceVo::getDeviceId, Collectors.toList()));
        return R.success(map,"获取成功");
    }

}
