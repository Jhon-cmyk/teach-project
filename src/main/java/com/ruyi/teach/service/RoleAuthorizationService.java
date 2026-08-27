package com.ruyi.teach.service;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.enums.UserRole;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

@Service
public class RoleAuthorizationService {

    public User requireAnyRole(User user, UserRole... allowedRoles) {
        UserRole actualRole = user == null
                ? null
                : UserRole.fromValue(user.getUserRole()).orElse(null);
        boolean allowed = actualRole != null
                && allowedRoles != null
                && Arrays.stream(allowedRoles).anyMatch(actualRole::equals);
        if (!allowed) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前账号没有执行该操作的权限");
        }
        return user;
    }

    public void requireOwner(User user, Long ownerId, String resourceName) {
        if (user == null || user.getId() == null || ownerId == null
                || !Objects.equals(user.getId(), ownerId)) {
            String safeResourceName = resourceName == null || resourceName.isBlank()
                    ? "资源"
                    : resourceName;
            throw new BusinessException(
                    ErrorCode.NO_AUTH_ERROR,
                    "只能操作本人创建的" + safeResourceName
            );
        }
    }
}
