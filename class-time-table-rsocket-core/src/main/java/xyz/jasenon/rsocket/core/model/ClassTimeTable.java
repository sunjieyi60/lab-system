package xyz.jasenon.rsocket.core.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Unique;

import java.io.Serializable;

/**
 * @description 班牌实体
 */
@Getter
@Setter
@TableName(value = "class_time_table",autoResultMap = true)
public class ClassTimeTable implements Serializable, Unique {
    /**
     * 主键id 主要防止页分裂
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 班牌唯一编号
     */
    private String uuid;
    /**
     * 班牌的配置信息
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Config config;
    /**
     * 关联的实验室id
     */
    private Long laboratoryId;

    private String status;

    @Override
    public String unique() {
        return uuid;
    }
}
