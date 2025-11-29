package xyz.jasenon.lab.service.exception;

/**
 * @author Jasenon_ce
 * @date 2025/11/29
 */
public class PermissionsInsufficientException extends RuntimeException {

    public PermissionsInsufficientException(String message) {
        super(message);
    }

}
