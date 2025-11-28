package xyz.jasenon.lab.common.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Command {

    /**
     * 命令行参数
     */
    private String commandLine;

    /**
     * 校验类型
     */
    private CheckType checkType;

    /**
     * 发送渠道
     */
    private SendType sendType;

}
