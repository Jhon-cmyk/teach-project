package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;
import com.ruyi.teach.model.dto.learning.StudentLearningContextRequest;
import com.ruyi.teach.model.entity.StudentLearningPreference;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.StudentLearningContextVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Locale;
import java.util.Set;

@Service
public class StudentLearningContextService {

    public static final String GOAL_POSTGRADUATE = "postgraduate";
    public static final String GOAL_EMPLOYMENT = "employment";
    public static final String GOAL_UNDECIDED = "undecided";
    private static final Set<String> VALID_GOALS = Set.of(
            GOAL_POSTGRADUATE, GOAL_EMPLOYMENT, GOAL_UNDECIDED
    );

    private final StudentLearningPreferenceMapper preferenceMapper;

    public StudentLearningContextService(StudentLearningPreferenceMapper preferenceMapper) {
        this.preferenceMapper = preferenceMapper;
    }

    @Transactional(readOnly = true)
    public StudentLearningContextVO getContext(User student) {
        requireStudent(student);
        return toVO(findGeneralPreference(student.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentLearningContextVO updateContext(StudentLearningContextRequest request, User student) {
        requireStudent(student);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学习背景不能为空");
        }
        String universityName = normalizeUniversityName(request.getUniversityName());
        String developmentGoal = normalizeDevelopmentGoal(request.getDevelopmentGoal());
        if (StringUtils.isBlank(universityName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择或填写所在大学");
        }
        if (StringUtils.isBlank(developmentGoal)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择发展目标");
        }

        StudentLearningPreference preference = findGeneralPreference(student.getId());
        if (preference == null) {
            preference = newGeneralPreference(student.getId(), new Date());
        }
        Date now = new Date();
        preference.setUniversityName(universityName);
        preference.setDevelopmentGoal(developmentGoal);
        preference.setUpdateTime(now);
        if (preference.getId() == null) {
            preferenceMapper.insert(preference);
        } else {
            preferenceMapper.updateById(preference);
        }
        return toVO(preference);
    }

    public StudentLearningPreference findGeneralPreference(Long studentId) {
        if (studentId == null) {
            return null;
        }
        return preferenceMapper.selectOne(new LambdaQueryWrapper<StudentLearningPreference>()
                .eq(StudentLearningPreference::getStudentId, studentId)
                .isNull(StudentLearningPreference::getCourseId)
                .last("limit 1"));
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentLearningPreference getOrCreateGeneralPreference(Long studentId) {
        if (studentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学生 ID 不能为空");
        }
        StudentLearningPreference preference = findGeneralPreference(studentId);
        if (preference != null) {
            return preference;
        }

        Date now = new Date();
        preference = newGeneralPreference(studentId, now);
        preferenceMapper.insert(preference);
        return preference;
    }

    private StudentLearningPreference newGeneralPreference(Long studentId, Date now) {
        StudentLearningPreference preference = new StudentLearningPreference();
        preference.setStudentId(studentId);
        preference.setDominantType("balanced");
        preference.setVideoScore(0);
        preference.setTextScore(0);
        preference.setPracticeScore(0);
        preference.setDiscussionScore(0);
        preference.setAiScore(0);
        preference.setResourceScore(0);
        preference.setProfileCompleted(0);
        preference.setAiQuestionCount(0);
        preference.setAiProfileSummary("");
        preference.setCreateTime(now);
        preference.setUpdateTime(now);
        return preference;
    }

    public boolean isComplete(StudentLearningPreference preference) {
        return preference != null
                && StringUtils.isNotBlank(preference.getUniversityName())
                && VALID_GOALS.contains(normalizeDevelopmentGoal(preference.getDevelopmentGoal()));
    }

    public String normalizeUniversityName(String value) {
        String normalized = StringUtils.normalizeSpace(StringUtils.trimToEmpty(value));
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    public String normalizeDevelopmentGoal(String value) {
        String normalized = StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT);
        return VALID_GOALS.contains(normalized) ? normalized : "";
    }

    private StudentLearningContextVO toVO(StudentLearningPreference preference) {
        StudentLearningContextVO vo = new StudentLearningContextVO();
        vo.setUniversityName(preference == null ? "" : StringUtils.defaultString(preference.getUniversityName()));
        vo.setDevelopmentGoal(preference == null ? "" : normalizeDevelopmentGoal(preference.getDevelopmentGoal()));
        vo.setComplete(isComplete(preference));
        return vo;
    }

    private void requireStudent(User student) {
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (!"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可维护学习背景");
        }
    }
}
