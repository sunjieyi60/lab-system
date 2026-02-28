package xyz.jasenon.lab.core.exception;

import xyz.jasenon.lab.core.exception.ImException;

/**
 * @ClassName ImDecodeException
 * @Description TODO
 * @Author WChao
 * @Date 2020/1/11 6:02
 * @Version 1.0
 **/
public class ImDecodeException extends ImException {

    public ImDecodeException(Throwable e) {
        super(e);
    }

    public ImDecodeException(String message) {
        super(message);

    }
}
