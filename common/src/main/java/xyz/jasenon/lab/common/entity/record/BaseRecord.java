package xyz.jasenon.lab.common.entity.record;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class BaseRecord {

    /**
     * id: 主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * createTime: 创建时间，自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * origin: 数据来源，非数据库字段
     */
    @TableField(exist = false)
    private Origin origin;

    /**
     * rs485Id: RS485网关ID，非数据库字段
     */
    private Long rs485Id;

}
