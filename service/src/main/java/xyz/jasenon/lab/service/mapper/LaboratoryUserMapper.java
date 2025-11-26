package xyz.jasenon.lab.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.common.entity.base.LaboratoryUser;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Mapper
public interface LaboratoryUserMapper extends BaseMapper<LaboratoryUser> {
}
