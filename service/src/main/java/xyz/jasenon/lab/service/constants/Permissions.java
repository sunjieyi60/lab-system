package xyz.jasenon.lab.service.constants;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.service.deserializer.PermissionSerializer;

@Slf4j
@JsonSerialize(using = PermissionSerializer.class)
@Getter
public enum Permissions {

    ROOT("根节点", 0, 0),
    USER("用户管理", ROOT.id, 1),
    USER_ADD("添加用户", USER.id, 2),
    USER_EDIT("编辑用户", USER.id, 3),
    USER_DELETE("删除用户", USER.id, 4),
    USER_VIEW("查看用户", USER.id, 5),
    PERMISSION("权限管理", ROOT.id, 6),
    PERMISSION_ADD("添加权限", PERMISSION.id, 7),
    PERMISSION_EDIT("编辑权限", PERMISSION.id, 8),
    PERMISSION_DELETE("删除权限", PERMISSION.id, 9),
    PERMISSION_VIEW("查看权限", PERMISSION.id, 10),
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
        return pathOf(permission, new ArrayList<>());
    }

    // dfs
    public static List<Integer> pathOf(Permissions permission, List<Integer> path) {
        if (permission == null) {
            throw new IllegalArgumentException("非法的权限类型");
        }
        if (permission.id != ROOT.id) {
            path.add(permission.id);
            return pathOf(map.get(permission.parentId), path);
        }
        path.add(ROOT.id);
        return path;
    }

    @Getter
    @Setter
    static class PermissionTree {
        Permissions parent;
        List<PermissionTree> children;
    }
}
