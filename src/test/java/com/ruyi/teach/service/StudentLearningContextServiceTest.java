package com.ruyi.teach.service;

import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;
import com.ruyi.teach.model.dto.learning.StudentLearningContextRequest;
import com.ruyi.teach.model.entity.StudentLearningPreference;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.StudentLearningContextVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentLearningContextServiceTest {

    private StudentLearningPreferenceMapper preferenceMapper;
    private StudentLearningContextService service;

    @BeforeEach
    void setUp() {
        preferenceMapper = mock(StudentLearningPreferenceMapper.class);
        service = new StudentLearningContextService(preferenceMapper);
    }

    @Test
    void createsIncompletePreferenceWithoutMarkingAssessmentComplete() {
        when(preferenceMapper.selectOne(any())).thenReturn(null);
        StudentLearningContextRequest request = new StudentLearningContextRequest();
        request.setUniversityName("  示例   大学  ");
        request.setDevelopmentGoal("employment");

        StudentLearningContextVO result = service.updateContext(request, student());

        assertThat(result.isComplete()).isTrue();
        assertThat(result.getUniversityName()).isEqualTo("示例 大学");
        verify(preferenceMapper).insert(any(StudentLearningPreference.class));
    }

    @Test
    void rejectsUnknownDevelopmentGoalDuringNormalization() {
        assertThat(service.normalizeDevelopmentGoal("abroad")).isEmpty();
        assertThat(service.normalizeDevelopmentGoal("POSTGRADUATE")).isEqualTo("postgraduate");
    }

    @Test
    void detectsLegacyProfileThatNeedsEnrichment() {
        StudentLearningPreference preference = new StudentLearningPreference();
        preference.setProfileCompleted(1);
        preference.setUniversityName("");
        preference.setDevelopmentGoal("");

        assertThat(service.isComplete(preference)).isFalse();
    }

    private User student() {
        User student = new User();
        student.setId(7L);
        student.setUserRole("student");
        return student;
    }
}
