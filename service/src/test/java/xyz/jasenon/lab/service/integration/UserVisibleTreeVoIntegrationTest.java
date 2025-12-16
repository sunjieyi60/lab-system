package xyz.jasenon.lab.service.integration;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.ServiceApplication;
import xyz.jasenon.lab.service.service.IUserService;
import xyz.jasenon.lab.service.vo.DeptVo;
import xyz.jasenon.lab.service.vo.UserBizVo;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ServiceApplication.class)
@Sql(scripts = "file:e:/lab-system/sql/db.sql")
class UserVisibleTreeVoIntegrationTest {

    @Autowired
    private IUserService userService;

    @Test
    void visibleTreeVo_joinResults_shouldContainDeptBuildingsAndLaboratories() {
        StpUtil.login(101L);
        R<UserBizVo> r = userService.getCurrentUserDetail();
        assertTrue(r.isOk());
        UserBizVo vo = r.getData();
        assertNotNull(vo);

        assertNotNull(vo.getDepts());
        assertEquals(1, vo.getDepts().size());
        DeptVo deptVo = vo.getDepts().get(0);
        assertEquals("计算机学院", deptVo.getDeptName());
        assertNotNull(deptVo.getBuildings());
        assertEquals(2, deptVo.getBuildings().size());

        assertNotNull(vo.getLaboratories());
        assertEquals(2, vo.getLaboratories().size());

        assertNotNull(vo.getPermissions());
    }
}
