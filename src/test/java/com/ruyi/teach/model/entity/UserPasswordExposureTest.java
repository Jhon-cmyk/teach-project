package com.ruyi.teach.model.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UserPasswordExposureTest {

    @Test
    void passwordIsExcludedFromJsonAndToString() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUserAccount("student001");
        user.setUserPassword("stored-password-hash");

        String json = new ObjectMapper().writeValueAsString(user);
        String text = user.toString();

        assertFalse(json.contains("userPassword"));
        assertFalse(json.contains("stored-password-hash"));
        assertFalse(text.contains("userPassword"));
        assertFalse(text.contains("stored-password-hash"));
    }
}
