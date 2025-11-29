package xyz.jasenon.lab.service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.service.IUserService;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private IUserService userService;

    @Test
    void testCreateUser_withAllPermissions() {
        var leafPermissions = Arrays.stream(Permissions.values())
                .filter(p -> Arrays.stream(Permissions.values())
                        .noneMatch(x -> x.getParentId().equals(p.getId())))
                .toList();
        CreateUser dto = new CreateUser()
                .setUsername("system")
                .setPassword("123456")
                .setRealName("系统")
                .setEmail("system@example.com")
                .setPhone("13900000000")
                .setCreateBy(1L)
                .setPermissions(leafPermissions)
                .setDeptIds(Collections.emptyList())
                .setLaboratoryIds(Collections.emptyList());

        when(userService.createUser(any(CreateUser.class))).thenReturn(R.success("OK"));

        R r = userController.createUser(dto);
        assertTrue(r.isOk());

        ArgumentCaptor<CreateUser> captor = ArgumentCaptor.forClass(CreateUser.class);
        verify(userService).createUser(captor.capture());
        CreateUser captured = captor.getValue();
        assertEquals("system", captured.getUsername());
        assertEquals("123456", captured.getPassword());
        assertNotNull(captured.getPermissions());
        assertEquals(leafPermissions.size(), captured.getPermissions().size());
    }
}