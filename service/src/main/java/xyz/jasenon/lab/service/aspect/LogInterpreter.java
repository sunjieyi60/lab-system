package xyz.jasenon.lab.service.aspect;

/**
 * 将指定类型的 payload 映射为可读日志文本或富内容。
 * 返回 String 时仅填充 content；返回 {@link xyz.jasenon.lab.service.dto.log.OperationLogParts} 时填充 room、device、operateWay、content。
 */
public interface LogInterpreter<T> {

    Object render(T payload);
}



