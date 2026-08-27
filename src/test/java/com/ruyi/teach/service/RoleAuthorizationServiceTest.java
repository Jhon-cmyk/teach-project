package com.ruyi.teach.service;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleAuthorizationServiceTest {

    private final RoleAuthorizationService service = new RoleAuthorizationService();

    @Test
    void acceptsOnlyExplicitlyAllowedRoles() {
        assertDoesNotThrow(() -> service.requireAnyRole(user(1L, "teacher"), UserRole.TEACHER));
        assertDoesNotThrow(() -> service.requireAnyRole(
                user(1L, "admin"),
                UserRole.TEACHER,
                UserRole.ADMIN
        ));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.requireAnyRole(user(1L, "student"), UserRole.TEACHER)
        );
        assertEquals(40101, exception.getCode());
    }

    @Test
    void rejectsUnknownOrMissingRoles() {
        assertThrows(
                BusinessException.class,
                () -> service.requireAnyRole(user(1L, "superuser"), UserRole.ADMIN)
        );
        assertThrows(
                BusinessException.class,
                () -> service.requireAnyRole(user(1L, null), UserRole.ADMIN)
        );
    }

    @Test
    void preventsCrossOwnerAccess() {
        assertDoesNotThrow(() -> service.requireOwner(user(7L, "teacher"), 7L, "课程"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.requireOwner(user(7L, "teacher"), 8L, "课程")
        );
        assertEquals(40101, exception.getCode());
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setUserRole(role);
        return user;
    }
}
