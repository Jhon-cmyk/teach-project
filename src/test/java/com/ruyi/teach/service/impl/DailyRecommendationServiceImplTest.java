package com.ruyi.teach.service.impl;

import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionMapper;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.StudentDailyRecommendationSession;
import com.ruyi.teach.model.entity.StudentResourceRecommendation;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;
import com.ruyi.teach.service.StudentLearningContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DailyRecommendationServiceImplTest {

    private DailyRecommendationServiceImpl service;
    private HomeworkAssignmentMapper homeworkAssignmentMapper;
    private HomeworkSubmissionMapper homeworkSubmissionMapper;
    private CourseMapper courseMapper;

    @BeforeEach
    void setUp() {
        service = new DailyRecommendationServiceImpl();
        homeworkAssignmentMapper = mock(HomeworkAssignmentMapper.class);
        homeworkSubmissionMapper = mock(HomeworkSubmissionMapper.class);
        courseMapper = mock(CourseMapper.class);
        ReflectionTestUtils.setField(service, "homeworkAssignmentMapper", homeworkAssignmentMapper);
        ReflectionTestUtils.setField(service, "homeworkSubmissionMapper", homeworkSubmissionMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "studentLearningContextService",
                new StudentLearningContextService(mock(StudentLearningPreferenceMapper.class)));
    }

    @Test
    void createsPendingHomeworkRecommendationWhenCourseIdIsNull() {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setId(101L);
        assignment.setClassId(7L);
        assignment.setCourseId(null);
        assignment.setTitle("Pending homework without a course");
        assignment.setDeadline(new Date(System.currentTimeMillis() + 60_000L));

        when(homeworkAssignmentMapper.selectList(any())).thenReturn(List.of(assignment));
        when(homeworkSubmissionMapper.selectList(any())).thenReturn(List.of());

        User student = new User();
        student.setId(5L);
        student.setClassId(7L);
        StudentDailyRecommendationSession session = new StudentDailyRecommendationSession();
        session.setStudentId(student.getId());

        List<StudentResourceRecommendation> recommendations = ReflectionTestUtils.invokeMethod(
                service,
                "pendingHomeworkRecommendations",
                session,
                student,
                new HashSet<String>()
        );

        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());
        assertEquals(assignment.getId(), recommendations.get(0).getResourceId());
        assertNull(recommendations.get(0).getCourseId());
        verifyNoInteractions(courseMapper);
    }

    @Test
    void givesDirectionScoreOnlyToMatchingCareerResources() {
        Integer employmentScore = ReflectionTestUtils.invokeMethod(
                service, "developmentDirectionScore", "Java 项目实战", "工程开发案例", "employment");
        Integer postgraduateScore = ReflectionTestUtils.invokeMethod(
                service, "developmentDirectionScore", "算法原理与真题", "系统复习", "postgraduate");
        Integer unrelatedScore = ReflectionTestUtils.invokeMethod(
                service, "developmentDirectionScore", "英语口语", "日常交流", "employment");

        assertNotNull(employmentScore);
        assertNotNull(postgraduateScore);
        assertEquals(50, employmentScore);
        assertEquals(50, postgraduateScore);
        assertEquals(0, unrelatedScore);
    }

    @Test
    void extractsExplicitDataStructureFocusFromNaturalLanguageDemand() {
        StudentDailyRecommendationSession session = new StudentDailyRecommendationSession();
        Set<String> focus = ReflectionTestUtils.invokeMethod(
                service,
                "explicitTopicFocus",
                session,
                new LinkedHashSet<>(List.of("今天想系统学习数据结构，重点看树和图"))
        );

        assertNotNull(focus);
        assertEquals(true, focus.contains("数据结构"));
        assertEquals(true, focus.contains("树"));
        assertEquals(true, focus.contains("图"));
        assertEquals(false, focus.contains("算法"));
    }

    @Test
    void selectedCourseDefinesTopicWhenSurveyAnswersAreGeneric() {
        com.ruyi.teach.model.entity.Course selectedCourse = new com.ruyi.teach.model.entity.Course();
        selectedCourse.setId(88L);
        selectedCourse.setName("数据结构");
        selectedCourse.setDescription("线性表、树和图");
        when(courseMapper.selectById(88L)).thenReturn(selectedCourse);

        StudentDailyRecommendationSession session = new StudentDailyRecommendationSession();
        session.setCourseId(88L);
        Set<String> focus = ReflectionTestUtils.invokeMethod(
                service,
                "explicitTopicFocus",
                session,
                new LinkedHashSet<>(List.of("查漏补缺"))
        );

        assertNotNull(focus);
        assertEquals(true, focus.contains("数据结构"));
        assertEquals(true, focus.contains("线性表"));
        assertEquals(true, focus.contains("树"));
        assertEquals(true, focus.contains("图"));
    }

    @Test
    void explicitTopicFocusRejectsUnrelatedSupplementalResources() {
        Set<String> focus = Set.of("数据结构");

        Boolean matched = ReflectionTestUtils.invokeMethod(
                service, "matchesTopicFocus", "Java 数据结构项目实战", "链表与哈希表实现", focus);
        Boolean unrelated = ReflectionTestUtils.invokeMethod(
                service, "matchesTopicFocus", "Java 零基础入门", "变量、循环与面向对象", focus);

        assertEquals(true, matched);
        assertEquals(false, unrelated);
    }

    @Test
    void fillsDailyRecommendationsToEightWithUniqueSameTopicEntries() {
        StudentDailyRecommendationSession session = new StudentDailyRecommendationSession();
        session.setStudentId(5L);
        List<StudentResourceRecommendation> recommendations = new ArrayList<>();

        ReflectionTestUtils.invokeMethod(
                service,
                "ensureDailyRecommendationMinimum",
                recommendations,
                session,
                "数据结构"
        );

        assertEquals(8, recommendations.size());
        assertEquals(8, recommendations.stream().map(StudentResourceRecommendation::getResourceTitle).distinct().count());
        assertEquals(true, recommendations.stream().allMatch(item -> "数据结构".equals(item.getKnowledgeName())));
        assertEquals(true, recommendations.stream().allMatch(item -> item.getCourseId() == null));
    }

    @Test
    void removesDuplicateCoursesIncludingDifferentVideoChapters() {
        StudentResourceRecommendation course = recommendation(1L, 20L, null, 20L, "video", "数据结构");
        StudentResourceRecommendation sameCourse = recommendation(2L, 20L, null, 99L, "micro_video", "数据结构系统课");
        StudentResourceRecommendation sameTitle = recommendation(3L, null, null, 30L, "text", "数据 结构");
        StudentResourceRecommendation chapterOne = recommendation(4L, 20L, 101L, 101L, "course_chapter_video", "数据结构 · 栈");
        StudentResourceRecommendation chapterTwo = recommendation(5L, 20L, 102L, 102L, "course_chapter_video", "数据结构 · 队列");

        List<StudentResourceRecommendation> result = ReflectionTestUtils.invokeMethod(
                service,
                "deduplicateRecommendations",
                List.of(course, sameCourse, sameTitle, chapterOne, chapterTwo)
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(List.of(1L), result.stream().map(StudentResourceRecommendation::getId).toList());
    }

    private StudentResourceRecommendation recommendation(
            Long id, Long courseId, Long chapterId, Long resourceId, String type, String title) {
        StudentResourceRecommendation recommendation = new StudentResourceRecommendation();
        recommendation.setId(id);
        recommendation.setCourseId(courseId);
        recommendation.setChapterId(chapterId);
        recommendation.setResourceId(resourceId);
        recommendation.setResourceType(type);
        recommendation.setResourceTitle(title);
        return recommendation;
    }
}
