package xyz.jasenon.rsocket.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

/**
 * @description 班牌的配置信息 主要是开门相关的配置
 */
@Getter
@Setter
public class Config {
    /**
     * 密码
     */
    private String password = "123456";
    /**
     * 人脸精度
     */
    private float facePrecision = 0.85f;
    /**
     * 验证页面超时时间
     */
    private Integer timeout = 30;
    /**
     * 超时时间单位
     */
    private TimeUnit unit = TimeUnit.SECONDS;

    public static Config Default(){
        return new Config();
    }

}
