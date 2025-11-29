package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateSemester;
import xyz.jasenon.lab.service.dto.course.DeleteSemester;
import xyz.jasenon.lab.service.dto.course.EditSemester;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
public interface ISemesterService extends IService<Semester> {

    R createSemester(CreateSemester createSemester);

    R deleteSemester(DeleteSemester deleteSemester);

    R editSemester(EditSemester editSemester);

    R<List<Semester>> listSemester();
}
