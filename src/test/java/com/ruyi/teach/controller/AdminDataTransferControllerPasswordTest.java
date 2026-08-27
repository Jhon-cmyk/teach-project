package com.ruyi.teach.controller;

import com.ruyi.teach.mapper.AdminImportBatchMapper;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.entity.AdminImportBatch;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.UserService;
import com.ruyi.teach.service.impl.PasswordServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDataTransferControllerPasswordTest {

    @Test
    void importedStudentPasswordUsesUnifiedBcryptService() throws Exception {
        PasswordServiceImpl passwordService = new PasswordServiceImpl(new BCryptPasswordEncoder());
        UserService userService = mock(UserService.class);
        AdminImportBatchMapper batchMapper = mock(AdminImportBatchMapper.class);
        AdminAuditLogger auditLogger = mock(AdminAuditLogger.class);

        AdminDataTransferController controller = new AdminDataTransferController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "passwordService", passwordService);
        ReflectionTestUtils.setField(controller, "sysClassMapper", mock(SysClassMapper.class));
        ReflectionTestUtils.setField(controller, "adminAuditLogger", auditLogger);
        ReflectionTestUtils.setField(controller, "adminImportBatchMapper", batchMapper);
        ReflectionTestUtils.setField(controller, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());

        when(userService.count(any())).thenReturn(0L);
        when(userService.save(any(User.class))).thenReturn(true);
        doAnswer(invocation -> {
            AdminImportBatch batch = invocation.getArgument(0);
            batch.setId(100L);
            return 1;
        }).when(batchMapper).insert(any(AdminImportBatch.class));

        MockHttpServletRequest request = new MockHttpServletRequest();
        User admin = new User();
        admin.setId(1L);
        admin.setUserAccount("admin001");
        admin.setUserName("管理员");
        admin.setUserRole("admin");
        request.getSession().setAttribute("user_login", admin);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                studentWorkbook()
        );

        controller.importStudents(file, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        User importedUser = userCaptor.getValue();
        assertTrue(passwordService.matches("34567890", importedUser.getUserPassword()));
        assertTrue(importedUser.getUserPassword().startsWith("$2"));
    }

    private byte[] studentWorkbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学生");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("专业");
            header.createCell(3).setCellValue("班级");
            header.createCell(4).setCellValue("学院");

            Row student = sheet.createRow(1);
            student.createCell(0).setCellValue("20261234567890");
            student.createCell(1).setCellValue("测试学生");

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
