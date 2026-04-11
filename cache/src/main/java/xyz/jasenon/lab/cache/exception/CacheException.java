package xyz.jasenon.lab.cache.exception;

/**
 * 缓存异常
 *
 * @author Jasenon
 */
public class CacheException extends RuntimeException {

    public CacheException(String message) {
        super(message);
    }

    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheException(Throwable cause) {
        super(cause);
    }
}
