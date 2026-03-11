package xyz.jasenon.lab.core.packets;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 电子班牌设备实体类
 * 用于全双工通信RPC系统中班牌设备的标识和管理
 * 
 * @author Jasenon_ce
 * @date 2026/2/28
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder(builderMethodName = "newBuilder")
public class ClassTimeTable extends Message implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备uuid（唯一标识）
     */
    private String uuid;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备状态（online/offline）
     */
    private String status = ClassTimeTableStatusType.OFFLINE.getStatus();

    /**
     * 终端类型（如：android、linux等）
     */
    private String terminal;

    /**
     * 设备IP地址
     */
    private String ip;

    /**
     * 设备所在群组列表（班牌支持多分组）
     */
    private List<Group> groups;

    /**
     * 设备配置信息
     */
    private Config config;

    /**
     * 获取班牌所属群组列表
     * @return List<Group> 群组列表
     */
    public List<Group> getGroups() {
        return groups;
    }

    /**
     * 设置班牌所属群组列表
     * @param groups 群组列表
     */
    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    /**
     * 浅拷贝克隆
     * @return ClassTimeTable 克隆对象
     */
    @Override
    public ClassTimeTable clone() {
        try {
            return (ClassTimeTable) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported for ClassTimeTable", e);
        }
    }

}
