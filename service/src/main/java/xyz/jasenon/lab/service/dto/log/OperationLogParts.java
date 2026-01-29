package xyz.jasenon.lab.service.dto.log;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 操作日志富内容：用于设备控制等需要填写 room、device、operateWay 的日志。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogParts {

    private String room;
    private String device;
    private String operateWay;
    private String content;
}
