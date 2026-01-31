package xyz.jasenon.lab.service.log;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.base.User;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.service.IDeptService;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import xyz.jasenon.lab.service.service.IUserService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账号管理操作日志解释器：添加/修改/删除用户等 content，含账号、角色、部门/实验室名称。
 */
@Component
@RequiredArgsConstructor
public class UserLogInterpreter {

    private final IUserService userService;
    private final IDeptService deptService;
    private final ILaboratoryService laboratoryService;

    public Object renderCreate(CreateUser payload) {
        String realName = payload.getRealName() != null ? payload.getRealName() : payload.getUsername();
        String username = payload.getUsername();
        String role = deriveRole(payload.getPermissions());
        String deptNames = resolveDeptNames(payload.getDeptIds());
        String labNames = resolveLaboratoryNames(payload.getLaboratoryIds());
        return buildContent("添加", realName, username, role, deptNames, labNames);
    }

    public Object renderEdit(EditUser payload) {
        String realName = payload.getRealName();
        if (realName == null && payload.getUserId() != null) {
            User u = userService.getById(payload.getUserId());
            realName = u != null ? (u.getRealName() != null ? u.getRealName() : u.getUsername()) : null;
        }
        if (realName == null) {
            realName = "ID " + payload.getUserId();
        }
        String role = deriveRole(payload.getPermissions());
        String deptNames = resolveDeptNames(payload.getDeptIds());
        String labNames = resolveLaboratoryNames(payload.getLaboratoryIds());
        return buildContent("修改", realName, null, role, deptNames, labNames);
    }

    public Object renderDelete(DeleteUser payload) {
        User u = userService.getById(payload.getUserId());
        String name = u != null ? (u.getRealName() != null ? u.getRealName() : u.getUsername()) : null;
        if (name == null) {
            name = "ID " + payload.getUserId();
        }
        return "删除、" + name;
    }

    private String buildContent(String action, String realName, String username, String role, String deptNames, String labNames) {
        List<String> parts = new ArrayList<>();
        parts.add(action);
        parts.add(realName != null ? realName : "");
        if (username != null && !username.isEmpty()) {
            parts.add(username);
        }
        if (role != null && !role.isEmpty()) {
            parts.add(role);
        }
        if (deptNames != null && !deptNames.isEmpty()) {
            parts.add(deptNames);
        }
        if (labNames != null && !labNames.isEmpty()) {
            parts.add(labNames);
        }
        return String.join("、", parts);
    }

    private String deriveRole(List<Permissions> perms) {
        if (perms == null || perms.isEmpty()) {
            return "普通用户";
        }
        for (Permissions p : perms) {
            if (p == null) continue;
            if (p == Permissions.ROOT) return "超级管理员";
            if (p == Permissions.USER || p == Permissions.BASE_SETTINGS || p == Permissions.CONTROL_CENTER
                    || p == Permissions.DATA_ANALYSIS || p == Permissions.ACADEMIC_AFFAIRS_MANAGEMENT) {
                return "管理员";
            }
        }
        return "普通用户";
    }

    private String resolveDeptNames(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return "";
        }
        return deptIds.stream()
                .map(id -> {
                    try {
                        var dept = deptService.getById(id);
                        return dept != null && dept.getDeptName() != null ? dept.getDeptName() : String.valueOf(id);
                    } catch (Exception e) {
                        return String.valueOf(id);
                    }
                })
                .collect(Collectors.joining("、"));
    }

    private String resolveLaboratoryNames(List<Long> laboratoryIds) {
        if (laboratoryIds == null || laboratoryIds.isEmpty()) {
            return "";
        }
        return laboratoryIds.stream()
                .map(id -> {
                    try {
                        Laboratory lab = laboratoryService.getById(id);
                        if (lab != null) {
                            return lab.getLaboratoryName() != null ? lab.getLaboratoryName() : (lab.getLaboratoryId() != null ? lab.getLaboratoryId() : String.valueOf(id));
                        }
                        return String.valueOf(id);
                    } catch (Exception e) {
                        return String.valueOf(id);
                    }
                })
                .collect(Collectors.joining("、"));
    }
}
