package xyz.jasenon.lab.service.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LaboratoryListVo {

    /**
     * 实验室id
     */
    private Long id;

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

    /**
     * 简介
     */
    private String intro;

    /**
     *  负责人
     */
    private String username;

    /**
     *  联系电话
     */
    private String phone;

}
