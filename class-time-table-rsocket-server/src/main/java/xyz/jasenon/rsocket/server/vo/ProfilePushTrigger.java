package xyz.jasenon.rsocket.server.vo;

/**
 * 管理端触发档案同步的类型
 */
public enum ProfilePushTrigger {
    /** 下发/更新班牌配置 */
    CONFIG,
    /** 修改关联实验室 */
    LABORATORY
}
