package xyz.jasenon.lab.service.quartz.check;

import xyz.jasenon.lab.common.entity.record.Origin;
import xyz.jasenon.lab.service.quartz.model.Data;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordFactory;

import java.text.MessageFormat;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
public class DataCollector {

    public static Data collect(Data data){
        var record = DeviceRecordFactory.getDeviceRecordMethod(data.getDeviceType()).getRecord(data.getDeviceId());
        data.setValue(record);
        return data;
    }

    public static Result<Boolean> check(Data data){
//        if (data.getValue().getData().getOrigin() == Origin.MySql){
//            return Result.error(false, MessageFormat.format("离线数据不可靠! 数据源为:{0}", data));
//        }
        return Result.success(true);
    }
}
