package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.LearningEventMapper;
import com.ruyi.teach.model.dto.learning.LearningEventBatchRequest;
import com.ruyi.teach.model.entity.LearningEvent;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.LearningHeatmapDayVO;
import com.ruyi.teach.service.LearningEventService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LearningEventServiceImpl extends ServiceImpl<LearningEventMapper, LearningEvent>
        implements LearningEventService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "resource_click", "video_watch", "video_pause", "video_rewatch",
            "comment_view", "comment_post", "ai_question", "practice_start",
            "practice_submit", "wrong_question_review"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchEvents(LearningEventBatchRequest request, User student) {
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (!"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可上报学习行为");
        }

        List<LearningEventBatchRequest.EventItem> items =
                request == null || request.getEvents() == null ? List.of() : request.getEvents();
        if (items.isEmpty()) {
            return true;
        }

        Date now = new Date();
        for (LearningEventBatchRequest.EventItem item : items) {
            if (item == null || !ALLOWED_TYPES.contains(StringUtils.trimToEmpty(item.getEventType()))) {
                continue;
            }
            LearningEvent event = new LearningEvent();
            event.setStudentId(student.getId());
            event.setClassId(student.getClassId());
            event.setCourseId(item.getCourseId());
            event.setChapterId(item.getChapterId());
            event.setResourceId(item.getResourceId());
            event.setResourceType(StringUtils.trimToEmpty(item.getResourceType()));
            event.setKnowledgeName(StringUtils.trimToEmpty(item.getKnowledgeName()));
            event.setEventType(StringUtils.trimToEmpty(item.getEventType()));
            event.setDurationSecond(nonNegative(item.getDurationSecond()));
            event.setScore(item.getScore());
            event.setCorrect(item.getCorrect() == null ? null : (item.getCorrect() > 0 ? 1 : 0));
            event.setExtraJson(item.getExtraJson());
            event.setEventTime(now);
            event.setCreateTime(now);
            event.setIsDelete(0);
            save(event);
        }
        return true;
    }

    @Override
    public List<LearningHeatmapDayVO> getLearningHeatmap(User student, Integer days) {
        requireStudent(student);
        int safeDays = days == null ? 180 : Math.max(1, Math.min(days, 366));
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);

        ZoneId zoneId = ZoneId.systemDefault();
        Date startTime = Date.from(startDate.atStartOfDay(zoneId).toInstant());
        Date endTimeExclusive = Date.from(endDate.plusDays(1).atStartOfDay(zoneId).toInstant());

        Map<String, Long> secondsByDate = new HashMap<>();
        List<Map<String, Object>> rows = baseMapper.sumDailyLearningSeconds(
                student.getId(),
                startTime,
                endTimeExclusive
        );
        for (Map<String, Object> row : rows) {
            secondsByDate.put(String.valueOf(row.get("studyDate")), toLong(row.get("totalSeconds")));
        }

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> toHeatmapDay(date, secondsByDate.getOrDefault(date.toString(), 0L)))
                .toList();
    }

    private void requireStudent(User student) {
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "璇峰厛鐧诲綍");
        }
        if (!"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "浠呭鐢熷彲鏌ョ湅瀛︿範鐑姏鍥?");
        }
    }

    private LearningHeatmapDayVO toHeatmapDay(LocalDate date, long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        LearningHeatmapDayVO day = new LearningHeatmapDayVO();
        day.setDate(date.toString());
        day.setSeconds(safeSeconds);
        day.setMinutes(Math.round(safeSeconds / 60.0));
        day.setHours(Math.round(safeSeconds / 360.0) / 10.0);
        return day;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.longValue();
        }
        if (value instanceof BigInteger integer) {
            return integer.longValue();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Integer nonNegative(Integer value) {
        return value == null ? null : Math.max(value, 0);
    }
}
