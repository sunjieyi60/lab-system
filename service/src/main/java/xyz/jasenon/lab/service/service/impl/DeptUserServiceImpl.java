package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.DeptUser;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.dept.BindUserToDept;
import xyz.jasenon.lab.service.mapper.DeptUserMapper;
import xyz.jasenon.lab.service.service.IDeptUserService;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Service
public class DeptUserServiceImpl extends ServiceImpl<DeptUserMapper, DeptUser> implements IDeptUserService {

    @Override
    public DeptUser userBindDept(BindUserToDept bindUserToDept) {
        Long doUserId = StpUtil.getLoginIdAsLong();

        boolean doesDoUserInThisDept = this.lambdaQuery()
                .eq(DeptUser::getUserId, doUserId)
                .eq(DeptUser::getDeptId, bindUserToDept.getDeptId())
                .exists();

        if (!doesDoUserInThisDept) {
            throw R.fail("你无权向该部门添加成员").convert();
        }

        // 创建部门用户关系
        DeptUser deptUser = new DeptUser();
        deptUser.setDeptId(bindUserToDept.getDeptId());
        deptUser.setUserId(bindUserToDept.getUserId());
        this.save(deptUser);
        return deptUser;
    }
}
