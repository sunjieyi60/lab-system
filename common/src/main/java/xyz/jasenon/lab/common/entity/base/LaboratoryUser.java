package xyz.jasenon.lab.common.entity.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Getter
@Setter
@Accessors(fluent = false, chain = true)
@TableName("laboratory_user")
public class LaboratoryUser extends BaseEntity {

    private Long userId;

    private Long laboratoryId;

}
