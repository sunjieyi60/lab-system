package xyz.jasenon.lab.common.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class Laboratory extends BaseEntity {

    /**
     * 实验室编号
     */
    private String laboratoryId;

    /**
     * 实验室名称
     */
    private String laboratoryName;

    /**
     * 实验室部门
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> belongToDepts;

    /**
     * 所属楼宇
     */
    private Long belongToBuilding;

    /**
     * 安全等级
     */
    private String securityLevel;

    /**
     * 容纳人数
     */
    private Integer classCapacity;

    /**
     * 面积
     */
    private Integer area;

}
