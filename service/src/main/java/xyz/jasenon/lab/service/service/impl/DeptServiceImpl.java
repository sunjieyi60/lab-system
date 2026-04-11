package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.Dept;
import xyz.jasenon.lab.common.entity.base.DeptUser;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.dept.CreateDept;
import xyz.jasenon.lab.service.dto.dept.DeleteDept;
import xyz.jasenon.lab.service.dto.dept.EditDept;
import xyz.jasenon.lab.service.mapper.DeptMapper;
import xyz.jasenon.lab.service.mapper.DeptUserMapper;
import xyz.jasenon.lab.service.service.IDeptService;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements IDeptService {

    @Autowired
    private DeptUserMapper deptUserMapper;

    @Override
    public Dept createDept(CreateDept createDept) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 创建部门
        Dept dept = new Dept();
        dept.setDeptName(createDept.getDeptName());
        this.save(dept);

        // 创建部门用户关系
        Long deptId = dept.getId();
        DeptUser deptUser = new DeptUser();
        deptUser.setDeptId(deptId);
        deptUser.setUserId(userId);
        deptUserMapper.insert(deptUser);
        return dept;
    }

    @Override
    public Dept editDept(EditDept editDept) {
        Dept dept = this.getById(editDept.getDeptId());
        if (dept == null) {
            throw R.fail("部门不存在").convert();
        }
        dept.setDeptName(editDept.getDeptName());
        this.updateById(dept);
        return dept;
    }

    @Override
    public void deleteDept(DeleteDept deleteDept) {
        Dept dept = this.getById(deleteDept.getDeptId());
        if (dept == null) {
            throw R.fail("部门不存在").convert();
        }
        this.removeById(deleteDept.getDeptId());
    }

    @Override
    public List<Dept> listDept() {
        return this.list();
    }
}
