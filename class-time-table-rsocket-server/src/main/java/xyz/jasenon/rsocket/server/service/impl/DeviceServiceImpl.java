package xyz.jasenon.rsocket.server.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.cache.Cache;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.core.packet.Heartbeat;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.server.mapper.DeviceMapper;
import xyz.jasenon.rsocket.server.service.DeviceService;

import java.util.List;

@Slf4j
@Service
public class DeviceServiceImpl extends MPJBaseServiceImpl<DeviceMapper, ClassTimeTable> implements DeviceService,Const.Key{

    @Autowired
    private Cache cache;

    @Override
    public Mono<RegisterResponse> register(RegisterRequest request) {
        return Mono.fromCallable(
                () ->{
                    log.info("Server 收到注册包 uuid:{}, laboratoryId:{}", request.getUuid(),request.getLaboratoryId());
                    var device = lambdaQuery().eq(ClassTimeTable::getUuid, request.getUuid()).one();

                    if (device == null){
                        var insert = new ClassTimeTable();
                        insert.setUuid(request.getUuid());
                        insert.setConfig(Config.Default());
                        insert.setLaboratoryId(request.getLaboratoryId());
                        save(insert);
                        device = insert;
                    }else{
                        if (device.getLaboratoryId() != null &&
                                !device.getLaboratoryId().equals(request.getLaboratoryId())){
                            device.setLaboratoryId(request.getLaboratoryId());
                            updateById(device);
                        }
                    }
                    // attention 这里的缓存清理有connect管理

                    // 包装响应类
                    var response = new RegisterResponse();
                    response.setConfig(device.getConfig() == null ? Config.Default() : device.getConfig());
                    response.setUuid(request.getUuid());
                    response.setStatus(Status.C10000);
                    response.setFrom(Const.Role.SERVER);
                    response.setTo(request.getUuid());

                    return response;
                }
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> heartbeat(Heartbeat heartbeat) {
        return null;
    }

    @Override
    public List<ClassTimeTable> listAll(String status) {
        if (!List.of(Const.Status.ONLINE, Const.Status.OFFLINE).contains(status)){
            throw R.badRequest("不合法的参数").convert();
        }
        return List.of();
    }
}
