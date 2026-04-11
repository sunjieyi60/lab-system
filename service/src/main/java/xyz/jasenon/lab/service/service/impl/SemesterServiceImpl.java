package xyz.jasenon.lab.service.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.cache.spi.Cache;
import xyz.jasenon.lab.common.Const;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateSemester;
import xyz.jasenon.lab.service.dto.course.DeleteSemester;
import xyz.jasenon.lab.service.dto.course.EditSemester;
import xyz.jasenon.lab.service.mapper.SemesterMapper;
import xyz.jasenon.lab.service.service.ISemesterService;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Service
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, Semester> implements ISemesterService, Const.Key {

    private final Cache cache;

    public SemesterServiceImpl(Cache cache) {
        this.cache = cache;
    }

    @Override
    public Semester createSemester(CreateSemester createSemester) {
        // 创建学期：参考项目风格，使用流式DTO取值
        Semester semester = new Semester();
        semester.setName(createSemester.getName());
        semester.setStartDate(createSemester.getStartDate());
        semester.setEndDate(createSemester.getEndDate());
        this.save(semester);
        return semester;
    }

    @Override
    public void deleteSemester(DeleteSemester deleteSemester) {
        Semester semester = this.getById(deleteSemester.getSemesterId());
        if (semester == null) {
            throw R.fail("学期不存在").convert();
        }
        this.removeById(deleteSemester.getSemesterId());
        // 回收缓存
        cache.delete(semesterInfo(deleteSemester.getSemesterId()));
    }

    @Override
    public Semester editSemester(EditSemester editSemester) {
        Semester semester = this.getById(editSemester.getSemesterId());
        if (semester == null) {
            throw R.badRequest("学期不存在").convert();
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
        // 清除缓存
        cache.delete(semesterInfo(semester.getId()));
        return semester;
    }

    @Override
    public List<Semester> listSemester() {
        return list();
    }
}
