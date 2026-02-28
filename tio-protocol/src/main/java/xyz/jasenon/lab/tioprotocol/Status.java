package xyz.jasenon.lab.tioprotocol;

/**
 * 状态接口
 * 参考 J-IM 的 Status 设计
 * 
 * <p>用于统一响应状态码管理</p>
 * 
 * @author Jasenon_ce
 */
public interface Status {
    
    /**
     * 获取状态码
     */
    int getCode();
    
    /**
     * 获取状态消息
     */
    String getMsg();
}
