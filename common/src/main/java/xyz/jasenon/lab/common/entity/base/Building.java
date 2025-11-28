package xyz.jasenon.lab.common.entity.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

@Getter
@Setter
@Accessors(chain = true)
@TableName("building")
public class Building extends BaseEntity {

    /**
     * 名称
     */
    private String buildingName;

}
