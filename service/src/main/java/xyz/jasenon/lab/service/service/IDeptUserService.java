package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.DeptUser;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.dept.BindUserToDept;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
public interface IDeptUserService extends IService<DeptUser> {

    R userBindDept(BindUserToDept bindUserToDept);

}
