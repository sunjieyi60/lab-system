package xyz.jasenon.lab.service.quartz.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.service.quartz.model.Action;

@Mapper
public interface ActionMapper extends MPJBaseMapper<Action> {
}
