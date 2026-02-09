package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.Dept;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.dept.CreateDept;
import xyz.jasenon.lab.service.dto.dept.DeleteDept;
import xyz.jasenon.lab.service.dto.dept.EditDept;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
public interface IDeptService extends IService<Dept> {

    R createDept(CreateDept createDept);

    R editDept(EditDept editDept);

    R deleteDept(DeleteDept deleteDept);

    /**
     * 获取部门列表
     */
    R listDept();

}
