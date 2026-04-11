package xyz.jasenon.lab.service.constants;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.service.deserializer.PermissionSerializer;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
@JsonSerialize(using = PermissionSerializer.class)
@Getter
public enum Permissions {

    ROOT("根节点", 0, 0),
    USER("用户管理", ROOT.id, 1),
    USER_ADD("添加用户", USER.id, 2),
    USER_EDIT("编辑用户", USER.id, 3),
    USER_DELETE("删除用户", USER.id, 4),
    ACADEMIC_AFFAIRS_MANAGEMENT("教务管理",ROOT.id,6),
    SCHEDULE_CLASSES("实验室排课",ACADEMIC_AFFAIRS_MANAGEMENT.id,7),
    SCHEDULE_CLASSES_VIEW("查看排课",ACADEMIC_AFFAIRS_MANAGEMENT.id,8),
    SEMESTER_SETTINGS("学期设置",ACADEMIC_AFFAIRS_MANAGEMENT.id,9),
    CONTROL_CENTER("控制中心",ROOT.id,10),
    DEVICE_ADD("添加设备",CONTROL_CENTER.id,11),
    DEVICE_CONTROL("设备控制",CONTROL_CENTER.id,12),
    DEVICE_SMART_CONTROL("智能控制",CONTROL_CENTER.id,13),
    DEVICE_ALARM_SETTINGS("报警设置",CONTROL_CENTER.id,14),
    DATA_ANALYSIS("数据分析",ROOT.id,15),
    ACADEMIC_AFFAIRS_ANALYSIS("教务数据",DATA_ANALYSIS.id,16),
    LABORATORY_POWER_CONSUMPTION("实验室能耗数据",DATA_ANALYSIS.id,17),
    LABORATORY_CENTRAL_AIRCONDITION("实验室中央空调数据",DATA_ANALYSIS.id,18),
    BASE_SETTINGS("基础设置",ROOT.id,19),
    BASE_CUD("增加、删除、修改",BASE_SETTINGS.id,20),
    BASE_VIEW("查看",BASE_SETTINGS.id,21),
    ;

    private final String description;
    private final Integer parentId;
    private final Integer id;
    public static final PermissionTree tree = treeAll();
    private static final Map<Integer, Permissions> map = mapAll();

    Permissions(String description, Integer parentId, Integer id) {
        this.description = description;
        this.parentId = parentId;
        this.id = id;
    }

    private static Map<Integer, Permissions> mapAll() {
        Map<Integer, Permissions> map = new HashMap<>();
        Permissions[] values = values();
        for (Permissions permission : values) {
            map.put(permission.id, permission);
        }
        return map;
    }

    private static boolean isDict(Permissions permission) {
        List<Permissions> childList = Arrays.asList(values())
                .stream().filter(p -> p.parentId == permission.id).toList();
        return !childList.isEmpty();
    }

    public static PermissionTree treeAll() {
        List<Permissions> allPermissions = Arrays.asList(values())
                .stream().sorted((p1, p2) -> {
                    return p1.parentId - p2.parentId;
                }).toList();

        log.info("all permissions:{}", allPermissions);
        allPermissions = new ArrayList<>(allPermissions);
        allPermissions.remove(ROOT);

        PermissionTree permissionTree = new PermissionTree();
        permissionTree.setParent(ROOT);
        permissionTree.setChildren(new ArrayList<>());

        List<Permissions> markList = new LinkedList<>(allPermissions);

        // bfs
        Queue<PermissionTree> help = new LinkedBlockingDeque<>();
        help.add(permissionTree);
        while (!help.isEmpty()) {
            PermissionTree poll = help.poll();
            for (Permissions permission : allPermissions) {
                if (Objects.equals(permission.parentId, poll.getParent().id) &&
                        markList.contains(permission)) {
                    markList.remove(permission);
                    PermissionTree child = new PermissionTree();
                    child.setParent(permission);
                    child.setChildren(new ArrayList<>());
                    poll.getChildren().add(child);
                    help.add(child);
                }
            }
        }
        return permissionTree;
    }

    public static List<Integer> pathOf(Permissions permission) {
        if (isDict(permission)) {
            throw new IllegalArgumentException("禁止传递权限目录");
        }
        return pathOf(permission, new ArrayList<>()).stream().sorted().toList();
    }

    // dfs
    public static List<Integer> pathOf(Permissions permission, List<Integer> path) {
        if (permission == null) {
            throw new IllegalArgumentException("非法的权限类型");
        }
        if (!permission.id.equals(ROOT.id)) {
            path.add(permission.id);
            return pathOf(map.get(permission.parentId), path);
        }
        path.add(ROOT.id);
        return path;
    }

    @Getter
    @Setter
    public static class PermissionTree {
        Permissions parent;
        List<PermissionTree> children;
    }
}
