package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.service.mapper.SemesterMapper;
import xyz.jasenon.lab.service.service.ISemesterService;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateSemester;
import xyz.jasenon.lab.service.dto.course.DeleteSemester;
import xyz.jasenon.lab.service.dto.course.EditSemester;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Service
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, Semester> implements ISemesterService {

    @Override
    public R createSemester(CreateSemester createSemester) {
        // 创建学期：参考项目风格，使用流式DTO取值
        Semester semester = new Semester();
        semester.setName(createSemester.getName());
        semester.setStartDate(createSemester.getStartDate());
        semester.setEndDate(createSemester.getEndDate());
        this.save(semester);
        return R.success("学期创建成功");
    }

    @Override
    public R deleteSemester(DeleteSemester deleteSemester) {
        Semester semester = this.getById(deleteSemester.getSemesterId());
        if (semester == null) {
            return R.fail("学期不存在");
        }
        this.removeById(deleteSemester.getSemesterId());
        return R.success("学期删除成功");
    }

    @Override
    public R editSemester(EditSemester editSemester) {
        Semester semester = this.getById(editSemester.getSemesterId());
        if (semester == null) {
            return R.fail("学期不存在");
        }
        // 编辑学期：使用 Hutool BeanUtil + CopyOptions 忽略空值与只读字段，保证风格与 LaboratoryServiceImpl 一致
        CopyOptions copyOptions = CopyOptions.create()
                .setIgnoreProperties("id", "createTime")
                .ignoreNullValue();

        Semester edit = new Semester();
        edit.setName(editSemester.getName());
        edit.setStartDate(editSemester.getStartDate());
        edit.setEndDate(editSemester.getEndDate());

        BeanUtil.copyProperties(edit, semester, copyOptions);
        this.updateById(semester);
        return R.success("学期修改成功");
    }

    @Override
    public R<List<Semester>> listSemester() {
        return R.success(list());
    }
}
