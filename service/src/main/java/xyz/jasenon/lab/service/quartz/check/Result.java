package xyz.jasenon.lab.service.quartz.check;

import lombok.AllArgsConstructor;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@AllArgsConstructor
public class Result<T> {
    private T data;
    private String message;

    public static <T> Result<T> success(T data) {
        return new Result<>(data, "success");
    }

    public static <T> Result<T> error(T data,String message) {
        return new Result<>(data, message);
    }
}
