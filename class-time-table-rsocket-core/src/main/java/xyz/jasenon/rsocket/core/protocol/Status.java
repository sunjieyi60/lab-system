package xyz.jasenon.rsocket.core.protocol;

public enum Status implements IStatus{

    C10000(10000,"ok","发送成功");

    private final Integer code;
    private final String desc;
    private final String msg;

    Status(Integer code, String desc, String msg) {
        this.code = code;
        this.desc = desc;
        this.msg = msg;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return desc + msg;
    }
}
