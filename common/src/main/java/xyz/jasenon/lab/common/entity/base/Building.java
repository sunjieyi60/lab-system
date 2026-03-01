package xyz.jasenon.lab.common.entity.base;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

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
