package xyz.jasenon.lab.service.quartz.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2026/1/9
 */
@Getter
@Setter
public class WatchDog {

    @NotNull(message = "看门狗必须设置是否启动")
    private Boolean watchEnabled;

    @NotNull(message = "看门狗必须设置间隔时间")
    private Integer watchIntervalSec;

    @NotNull(message = "看门狗必须设置超时时间")
    private Integer watchTimeoutSec;

    @NotNull(message = "看门狗必须设置是否在第一次成功时是否停止")
    private Boolean stopOnFirstSuccess;

}
