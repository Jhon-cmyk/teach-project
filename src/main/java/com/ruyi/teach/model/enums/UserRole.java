package com.ruyi.teach.model.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UserRole {
    STUDENT("student"),
    TEACHER("teacher"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<UserRole> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(role -> role.value.equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
