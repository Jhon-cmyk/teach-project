package com.ruyi.teach.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.controller.SessionUserContext;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseChapterMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionMapper;
import com.ruyi.teach.mapper.TeacherRegistrationCodeMapper;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.mapper.UserMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.TeacherRegistrationCode;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.AgentIndexService;
import com.ruyi.teach.service.DeepSeekService;
import com.ruyi.teach.service.PasswordService;
import com.ruyi.teach.service.TeachingCaseAssetService;
import com.ruyi.teach.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class CoreBusinessFlowIntegrationTest {

    private static final String TEST_DATABASE_NAME = "teach_core_flow_test";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName(TEST_DATABASE_NAME)
            .withUsername("teach_test")
            .withPassword("teach_test")
            .withReuse(false);

    @DynamicPropertySource
    static void configureIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseChapterMapper courseChapterMapper;

    @Autowired
    private TeacherRegistrationCodeMapper teacherRegistrationCodeMapper;

    @Autowired
    private AiResourceMapper aiResourceMapper;

    @Autowired
    private HomeworkSubmissionMapper homeworkSubmissionMapper;

    @Autowired
    private TeachingCaseMapper teachingCaseMapper;

    @MockitoBean
    private AgentIndexService agentIndexService;

    @MockitoBean
    private TeachingCaseAssetService teachingCaseAssetService;

    @MockitoBean
    private AdminAuditLogger adminAuditLogger;

    @MockitoBean
    private DeepSeekService deepSeekService;

    @Test
    void studentCanRegisterLoginAndLogoutWhileDuplicateRegistrationIsRejected() throws Exception {
        String account = "flow_student";
        String rawLoginInput = "FlowPass123";
        String changedPassword = "FlowPass456";
        String registrationBody = objectMapper.createObjectNode()
                .put("userAccount", account)
                .put("userPassword", rawLoginInput)
                .put("checkPassword", rawLoginInput)
                .put("userName", "流程学生")
                .put("userRole", "student")
                .toString();

        MvcResult registration = mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long userId = responseJson(registration).path("data").asLong();

        User storedUser = userMapper.selectById(userId);
        assertThat(storedUser).isNotNull();
        assertThat(storedUser.getUserRole()).isEqualTo("student");
        assertThat(passwordService.matches(rawLoginInput, storedUser.getUserPassword())).isTrue();

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(jsonPath("$.code").value(40000));

        MockHttpSession session = new MockHttpSession();
        MvcResult captcha = mockMvc.perform(get("/user/captcha")
                        .session(session)
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.captchaId").isNotEmpty())
                .andExpect(jsonPath("$.data.captchaCode").isNotEmpty())
                .andReturn();
        JsonNode captchaData = responseJson(captcha).path("data");

        String loginBody = objectMapper.createObjectNode()
                .put("userAccount", account)
                .put("userPassword", rawLoginInput)
                .put("captchaId", captchaData.path("captchaId").asText())
                .put("captchaCode", captchaData.path("captchaCode").asText())
                .toString();

        mockMvc.perform(post("/user/login/captcha")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.userRole").value("student"))
                .andExpect(jsonPath("$.data.userPassword").doesNotExist());

        String changePasswordBody = objectMapper.createObjectNode()
                .put("oldPassword", rawLoginInput)
                .put("newPassword", changedPassword)
                .toString();
        mockMvc.perform(post("/user/update/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
        assertThat(passwordService.matches(
                changedPassword,
                userMapper.selectById(userId).getUserPassword()
        )).isTrue();

        mockMvc.perform(get("/user/get/login").session(session))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(userId));

        mockMvc.perform(post("/user/logout").session(session))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/user/get/login"))
                .andExpect(jsonPath("$.code").value(40100));

        MockHttpSession oldPasswordSession = new MockHttpSession();
        JsonNode oldPasswordCaptcha = fetchLocalCaptcha(oldPasswordSession);
        String oldPasswordLoginBody = objectMapper.createObjectNode()
                .put("userAccount", account)
                .put("userPassword", rawLoginInput)
                .put("captchaId", oldPasswordCaptcha.path("captchaId").asText())
                .put("captchaCode", oldPasswordCaptcha.path("captchaCode").asText())
                .toString();
        mockMvc.perform(post("/user/login/captcha")
                        .session(oldPasswordSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oldPasswordLoginBody))
                .andExpect(jsonPath("$.code").value(40000));

        MockHttpSession changedPasswordSession = new MockHttpSession();
        JsonNode changedPasswordCaptcha = fetchLocalCaptcha(changedPasswordSession);
        String changedPasswordLoginBody = objectMapper.createObjectNode()
                .put("userAccount", account)
                .put("userPassword", changedPassword)
                .put("captchaId", changedPasswordCaptcha.path("captchaId").asText())
                .put("captchaCode", changedPasswordCaptcha.path("captchaCode").asText())
                .toString();
        mockMvc.perform(post("/user/login/captcha")
                        .session(changedPasswordSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changedPasswordLoginBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(userId));
    }

    @Test
    void teacherRegistrationConsumesIssuedCodeAndRejectsReuse() throws Exception {
        TeacherRegistrationCode registrationCode = new TeacherRegistrationCode();
        registrationCode.setRegisterCode("FLOW-TEACHER-001");
        registrationCode.setTeacherName("流程教师");
        registrationCode.setTeacherTitle("讲师");
        registrationCode.setStatus("unused");
        registrationCode.setIsDelete(0);
        teacherRegistrationCodeMapper.insert(registrationCode);

        String body = objectMapper.createObjectNode()
                .put("userAccount", "flow_registered_teacher")
                .put("userPassword", "TeacherPass123")
                .put("checkPassword", "TeacherPass123")
                .put("userName", "流程教师")
                .put("userRole", "teacher")
                .put("teacherRegisterCode", registrationCode.getRegisterCode())
                .toString();

        MvcResult registration = mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long teacherId = responseJson(registration).path("data").asLong();

        User teacher = userMapper.selectById(teacherId);
        assertThat(teacher.getUserRole()).isEqualTo("teacher");
        assertThat(teacher.getTeacherTitle()).isEqualTo("讲师");
        assertThat(teacher.getTeacherRegisterCode()).isEqualTo(registrationCode.getRegisterCode());

        TeacherRegistrationCode consumedCode =
                teacherRegistrationCodeMapper.selectById(registrationCode.getId());
        assertThat(consumedCode.getStatus()).isEqualTo("used");
        assertThat(consumedCode.getUsedBy()).isEqualTo(teacherId);
        assertThat(consumedCode.getUsedTime()).isNotNull();

        String reusedCodeBody = objectMapper.createObjectNode()
                .put("userAccount", "flow_second_teacher")
                .put("userPassword", "TeacherPass456")
                .put("checkPassword", "TeacherPass456")
                .put("userName", "第二位教师")
                .put("userRole", "teacher")
                .put("teacherRegisterCode", registrationCode.getRegisterCode())
                .toString();
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reusedCodeBody))
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void successfulLegacyPasswordLoginMigratesStoredHashToBcrypt() {
        String rawLoginInput = "LegacyPass123";
        User legacyUser = new User();
        legacyUser.setUserAccount("legacy_flow_user");
        legacyUser.setUserPassword(org.springframework.util.DigestUtils.md5DigestAsHex(
                ("ruyi_teach" + rawLoginInput).getBytes(StandardCharsets.UTF_8)
        ));
        legacyUser.setUserName("旧密码用户");
        legacyUser.setUserRole("student");
        legacyUser.setIsDelete(0);
        userMapper.insert(legacyUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        User loginUser = userService.userLogin(legacyUser.getUserAccount(), rawLoginInput, request);

        User migrated = userMapper.selectById(legacyUser.getId());
        assertThat(loginUser.getId()).isEqualTo(legacyUser.getId());
        assertThat(migrated.getUserPassword()).startsWith("$2");
        assertThat(passwordService.matches(rawLoginInput, migrated.getUserPassword())).isTrue();
        assertThat(SessionUserContext.getOptional(request)).isNotNull();
    }

    @Test
    void studentTeacherAndAdminRoutesEnforceTheirOwnRoleBoundaries() throws Exception {
        User student = persistUser("role_student", "student", 1001L);
        User teacher = persistUser("role_teacher", "teacher", null);
        User admin = persistUser("role_admin", "admin", null);

        mockMvc.perform(get("/admin/dashboard/metrics").session(sessionFor(student)))
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(get("/teacher/dashboard/stats").session(sessionFor(teacher)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin/dashboard/metrics").session(sessionFor(admin)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/homework/student/pending").session(sessionFor(admin)))
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void teacherCanCreateAndUpdateOwnCourseButOtherRolesCannotModifyIt() throws Exception {
        User owner = persistUser("course_owner", "teacher", null);
        User otherTeacher = persistUser("course_other", "teacher", null);
        User student = persistUser("course_student", "student", 1002L);

        com.fasterxml.jackson.databind.node.ObjectNode createPayload = objectMapper.createObjectNode();
        createPayload.put("name", "集成测试课程");
        createPayload.put("description", "初始课程说明");
        createPayload.put("type", "video");
        createPayload.putArray("classIds");
        String createBody = createPayload.toString();

        MvcResult created = mockMvc.perform(post("/course/add")
                        .session(sessionFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long courseId = responseJson(created).path("data").asLong();

        Course storedCourse = courseMapper.selectById(courseId);
        assertThat(storedCourse.getTeacherId()).isEqualTo(owner.getId());
        assertThat(storedCourse.getName()).isEqualTo("集成测试课程");

        com.fasterxml.jackson.databind.node.ObjectNode updatePayload = objectMapper.createObjectNode();
        updatePayload.put("id", courseId);
        updatePayload.put("name", "集成测试课程（已更新）");
        updatePayload.put("description", "修改后的课程说明");
        updatePayload.putArray("classIds");
        String updateBody = updatePayload.toString();

        mockMvc.perform(post("/course/update")
                        .session(sessionFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        assertThat(courseMapper.selectById(courseId).getName()).isEqualTo("集成测试课程（已更新）");

        String chapterCreateBody = objectMapper.createObjectNode()
                .put("courseId", courseId)
                .put("title", "第一章 集成测试")
                .put("sortOrder", 1)
                .put("animHtml", "<section>章节内容</section>")
                .toString();
        MvcResult chapterCreated = mockMvc.perform(post("/chapter/add")
                        .session(sessionFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chapterCreateBody))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long chapterId = responseJson(chapterCreated).path("data").asLong();

        mockMvc.perform(get("/chapter/list").param("courseId", String.valueOf(courseId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(chapterId))
                .andExpect(jsonPath("$.data[0].title").value("第一章 集成测试"));

        String chapterUpdateBody = objectMapper.createObjectNode()
                .put("id", chapterId)
                .put("title", "第一章 集成测试（已更新）")
                .put("sortOrder", 2)
                .toString();
        mockMvc.perform(post("/chapter/update")
                        .session(sessionFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chapterUpdateBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
        CourseChapter updatedChapter = courseChapterMapper.selectById(chapterId);
        assertThat(updatedChapter.getTitle()).isEqualTo("第一章 集成测试（已更新）");
        assertThat(updatedChapter.getSortOrder()).isEqualTo(2);

        mockMvc.perform(post("/course/update")
                        .session(sessionFor(otherTeacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(post("/chapter/update")
                        .session(sessionFor(otherTeacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chapterUpdateBody))
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(post("/chapter/add")
                        .session(sessionFor(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chapterCreateBody))
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(post("/course/add")
                        .session(sessionFor(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void approvedPlatformCaseIsPersistedAndSentToAgentIndex() throws Exception {
        User admin = persistUser("case_admin", "admin", null);
        User teacher = persistUser("case_teacher", "teacher", null);
        when(teachingCaseAssetService.rebuildCaseImages(any(TeachingCase.class))).thenReturn(List.of());

        String body = objectMapper.createObjectNode()
                .put("title", "二叉树课堂案例")
                .put("category", "course_design")
                .put("difficulty", "medium")
                .put("courseName", "数据结构")
                .put("pdfUrl", "file:///target/missing-case.pdf")
                .put("summary", "用于验证案例写入和索引同步")
                .put("keywords", "二叉树,遍历")
                .put("status", "approved")
                .toString();

        MvcResult imported = mockMvc.perform(post("/admin/teaching-case/import")
                        .session(sessionFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long caseId = responseJson(imported).path("data").asLong();

        TeachingCase storedCase = teachingCaseMapper.selectById(caseId);
        assertThat(storedCase.getScope()).isEqualTo("platform");
        assertThat(storedCase.getStatus()).isEqualTo("approved");
        verify(agentIndexService).upsertTeachingCase(any(TeachingCase.class));

        mockMvc.perform(post("/admin/teaching-case/import")
                        .session(sessionFor(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void teacherPublishesStudentSubmitsAndTeacherReviewsHomework() throws Exception {
        long classId = 2001L;
        User teacher = persistUser("homework_teacher", "teacher", null);
        User student = persistUser("homework_student", "student", classId);
        User wrongClassStudent = persistUser("homework_other_student", "student", 2002L);

        jdbcTemplate.update(
                "INSERT INTO sys_class (id, name, major, college) VALUES (?, ?, ?, ?)",
                classId, "集成测试教学班", "软件工程", "测试学院");
        jdbcTemplate.update("""
                INSERT INTO teacher_schedule
                    (teacher_id, course_name, class_name, week_start, week_end, day_of_week,
                     start_period, end_period, semester_label, is_delete)
                VALUES (?, ?, ?, 1, 18, 1, 1, 2, '2026-2027-1', 0)
                """, teacher.getId(), "集成测试课程", "集成测试教学班");

        AiResource quiz = new AiResource();
        quiz.setTeacherId(teacher.getId());
        quiz.setType("quiz");
        quiz.setTitle("基础算术作业");
        quiz.setContent("""
                一、单项选择题
                1. 2 + 2 的结果是？
                A. 4
                B. 5

                ---
                参考答案与解析
                1. 答案：A
                """);
        quiz.setParamsJson("{\"totalCount\":1,\"totalScore\":100}");
        quiz.setIsPublished(0);
        quiz.setIsDelete(0);
        aiResourceMapper.insert(quiz);

        String publishBody = objectMapper.createObjectNode()
                .put("quizResourceId", quiz.getId())
                .put("classId", classId)
                .put("title", "第一次数学作业")
                .put("assignmentType", "homework")
                .put("answerMode", "online")
                .put("gradingMode", "auto")
                .put("maxAttemptCount", 1)
                .toString();

        String unauthorizedPublishBody = objectMapper.createObjectNode()
                .put("quizResourceId", quiz.getId())
                .put("classId", 2002L)
                .put("title", "不应发布到非授课班级")
                .put("assignmentType", "homework")
                .toString();
        mockMvc.perform(post("/homework/assignment/publish")
                        .session(sessionFor(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unauthorizedPublishBody))
                .andExpect(jsonPath("$.code").value(40101));

        MvcResult published = mockMvc.perform(post("/homework/assignment/publish")
                        .session(sessionFor(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long assignmentId = responseJson(published).path("data").asLong();

        String answerJson = """
                [{"num":"1","originalQuestionNo":"1","type":"radio","stem":"2 + 2 的结果是？","answer":"A"}]
                """;
        String submitBody = objectMapper.createObjectNode()
                .put("assignmentId", assignmentId)
                .put("submissionType", "online")
                .put("studentAnswerJson", answerJson)
                .toString();

        MvcResult submitted = mockMvc.perform(post("/homework/submission/submit")
                        .session(sessionFor(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long submissionId = responseJson(submitted).path("data").asLong();

        HomeworkSubmission storedSubmission = homeworkSubmissionMapper.selectById(submissionId);
        assertThat(storedSubmission.getStudentId()).isEqualTo(student.getId());
        assertThat(storedSubmission.getSubmitStatus()).isEqualTo("review_pending");
        assertThat(storedSubmission.getReviewStatus()).isEqualTo("pending");

        Long detailId = jdbcQueryForLong(
                "SELECT id FROM homework_submission_detail WHERE submissionId = ? ORDER BY id LIMIT 1",
                submissionId
        );
        assertThat(detailId).isNotNull();

        com.fasterxml.jackson.databind.node.ObjectNode reviewPayload = objectMapper.createObjectNode();
        reviewPayload.put("submissionId", submissionId);
        reviewPayload.put("teacherRemark", "作答正确，继续保持。");
        reviewPayload.putArray("details")
                .add(objectMapper.createObjectNode().put("id", detailId).put("score", 100));
        String reviewBody = reviewPayload.toString();

        mockMvc.perform(post("/homework/teacher/submission/review")
                        .session(sessionFor(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        HomeworkSubmission reviewed = homeworkSubmissionMapper.selectById(submissionId);
        assertThat(reviewed.getSubmitStatus()).isEqualTo("submitted");
        assertThat(reviewed.getReviewStatus()).isEqualTo("approved");
        assertThat(reviewed.getTotalScore()).isEqualTo(100);

        mockMvc.perform(post("/homework/submission/submit")
                        .session(sessionFor(wrongClassStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Long jdbcQueryForLong(String sql, Object... args) {
        List<Long> values = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
        return values.isEmpty() ? null : values.getFirst();
    }

    private User persistUser(String account, String role, Long classId) {
        User user = new User();
        user.setUserAccount(account);
        user.setUserPassword(passwordService.encode("TestPass123"));
        user.setUserName(account);
        user.setUserRole(role);
        user.setClassId(classId);
        user.setIsDelete(0);
        userMapper.insert(user);
        return user;
    }

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUserContext.SESSION_USER_KEY, user);
        return session;
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode fetchLocalCaptcha(MockHttpSession session) throws Exception {
        MvcResult captcha = mockMvc.perform(get("/user/captcha")
                        .session(session)
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.captchaId").isNotEmpty())
                .andExpect(jsonPath("$.data.captchaCode").isNotEmpty())
                .andReturn();
        return responseJson(captcha).path("data");
    }
}
