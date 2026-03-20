package xyz.jasenon.rsocket.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;

import java.util.List;

/**
 * 班牌设备 Mapper
 */
@Mapper
public interface ClassTimeTableMapper extends BaseMapper<ClassTimeTable> {

    /**
     * 根据 uuid 查询
     */
    @Select("SELECT * FROM class_time_table_device WHERE uuid = #{uuid} AND deleted = 0")
    ClassTimeTable selectByUuid(@Param("uuid") String uuid);

    /**
     * 根据实验室ID查询
     */
    @Select("SELECT * FROM class_time_table_device WHERE laboratory_id = #{laboratoryId} AND deleted = 0")
    List<ClassTimeTable> selectByLaboratoryId(@Param("laboratoryId") Long laboratoryId);

    /**
     * 更新在线状态
     */
    @Update("UPDATE class_time_table_device SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 查询所有在线设备
     */
    @Select("SELECT * FROM class_time_table_device WHERE status = '" + Const.Status.ONLINE + "' AND deleted = 0")
    List<ClassTimeTable> selectOnlineDevices();
}
