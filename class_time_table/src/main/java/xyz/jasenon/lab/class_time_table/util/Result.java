package xyz.jasenon.lab.class_time_table.util;
/**
* @author Jasenon_ce
* @date 2026/2/4 
*/
public class Result <T>{

    private T data;
    private String message;
    private Integer code;

    public Result(T data, String message, Integer code) {
        this.data = data;
        this.message = message;
        this.code = code;
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<T>(data, message, 200);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<T>(null, message, code);
    }

}
