package xyz.jasenon.lab.service.aspect;

/**
 * 将指定类型的 payload 映射为可读日志文本。
 */
public interface LogInterpreter<T> {

    String render(T payload);
}

