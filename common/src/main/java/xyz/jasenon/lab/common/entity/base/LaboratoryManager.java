package xyz.jasenon.lab.common.entity.base;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Getter
@Setter
@Accessors(chain = true)
@TableName("laboratory_manager")
public class LaboratoryManager extends BaseEntity {

    /**
     * 实验室编号
     */
    private Long laboratoryId;

    /**
     * 用户ID
     */
    private Long userId;

}
