package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.Dept;
import xyz.jasenon.lab.common.entity.base.DeptUser;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.CreateDept;
import xyz.jasenon.lab.service.dto.DeleteDept;
import xyz.jasenon.lab.service.dto.EditDept;
import xyz.jasenon.lab.service.mapper.DeptMapper;
import xyz.jasenon.lab.service.mapper.DeptUserMapper;
import xyz.jasenon.lab.service.service.IDeptService;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements IDeptService {

    @Autowired
    private DeptUserMapper deptUserMapper;

    @Override
    public R createDept(CreateDept createDept) {
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

        return R.success("创建部门成功");
    }

    @Override
    public R editDept(EditDept editDept) {
        Dept dept = this.getById(editDept.deptId());
        if (dept == null) {
            return R.fail("部门不存在");
        }
        dept.setDeptName(editDept.deptName());
        this.updateById(dept);
        return R.success("编辑部门成功");
    }

    @Override
    public R deleteDept(DeleteDept deleteDept) {
        Dept dept = this.getById(deleteDept.getDeptId());
        if (dept == null) {
            return R.fail("部门不存在");
        }
        this.removeById(deleteDept.getDeptId());
        return R.success("删除部门成功");
    }
}
