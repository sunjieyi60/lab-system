package xyz.jasenon.lab.common.entity.base;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;

/**
 * @author Jasenon_ce
 */
@Getter
@Setter
@TableName("dept")
public class Dept extends BaseEntity {

    /**
     * 部门名称
     */
    private String deptName;

}
