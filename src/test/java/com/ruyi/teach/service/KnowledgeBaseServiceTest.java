package com.ruyi.teach.service;

import com.ruyi.teach.client.XfyunKnowledgeBaseClient;
import com.ruyi.teach.config.XfyunKnowledgeBaseProperties;
import com.ruyi.teach.mapper.CourseClassRelationMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.model.dto.TutorChatRequest;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.knowledge.KnowledgeFileVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeSearchHitVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseServiceTest {

    private XfyunKnowledgeBaseClient client;
    private CourseMapper courseMapper;
    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        client = mock(XfyunKnowledgeBaseClient.class);
        courseMapper = mock(CourseMapper.class);
        XfyunKnowledgeBaseProperties properties = new XfyunKnowledgeBaseProperties();
        properties.setEnabled(true);
        properties.setAppId("app-id");
        properties.setSecret("secret");
        service = new KnowledgeBaseService(
                client,
                properties,
                courseMapper,
                mock(CourseClassRelationMapper.class)
        );
    }

    @Test
    void retrievesBoundCourseEvidenceAndProducesTraceablePrompt() {
        Course course = new Course();
        course.setId(12L);
        course.setName("数据结构");
        course.setSourceType("platform");
        course.setPublishStatus("published");
        course.setIsDelete(0);
        course.setKnowledgeRepoId("repo-12");
        course.setKnowledgeRepoName("数据结构课程知识库");
        when(courseMapper.selectById(12L)).thenReturn(course);
        when(client.search("repo-12", "二叉树遍历有什么区别？")).thenReturn(List.of(
                new KnowledgeSearchHitVO("先序遍历按照根、左、右访问节点。", 91.2D,
                        "file-1", null, 1, "text", "md")
        ));
        when(client.listFiles("repo-12")).thenReturn(List.of(
                new KnowledgeFileVO("file-1", "数据结构考研理论.md", "text", "vectored",
                        "md", 18, null, null, null)
        ));

        TutorChatRequest request = new TutorChatRequest();
        request.setCourseId(12L);
        request.setMessage("二叉树遍历有什么区别？");

        KnowledgeBaseService.RetrievalContext result = service.retrieveForStudent(student(), request);

        assertThat(result.hasEvidence()).isTrue();
        assertThat(result.promptContext())
                .contains("课程：数据结构", "[资料1]", "数据结构考研理论.md", "先序遍历")
                .contains("证据不足时明确说明");
        assertThat(result.eventMetadata())
                .containsEntry("knowledgeBaseUsed", true)
                .containsEntry("knowledgeCourseId", 12L)
                .containsEntry("knowledgeHitCount", 1);
        verify(client).search("repo-12", "二叉树遍历有什么区别？");
    }

    private User student() {
        User student = new User();
        student.setId(7L);
        student.setClassId(1L);
        student.setUserRole("student");
        return student;
    }
}
