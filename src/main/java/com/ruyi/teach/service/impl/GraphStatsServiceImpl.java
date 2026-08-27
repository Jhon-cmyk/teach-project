package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.mapper.*;
import com.ruyi.teach.model.entity.*;
import com.ruyi.teach.service.GraphStatsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class GraphStatsServiceImpl implements GraphStatsService {

    @Resource
    private CourseGraphNodeMapper courseGraphNodeMapper;

    @Resource
    private CourseGraphNodeActivityMapper courseGraphNodeActivityMapper;

    @Resource
    private CourseGraphNodeProgressMapper courseGraphNodeProgressMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private HomeworkAssignmentMapper homeworkAssignmentMapper;

    @Resource
    private HomeworkSubmissionMapper homeworkSubmissionMapper;

    @Resource
    private CodingProblemMapper codingProblemMapper;

    @Resource
    private CodingSubmissionMapper codingSubmissionMapper;

    @Override
    public Map<String, Object> getOverview(Long teacherId, Long classId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取班级学生
        List<Long> studentIds = getStudentIdsByClass(classId);
        result.put("studentCount", studentIds.size());

        // 2. 获取所有知识点节点
        List<CourseGraphNode> nodes = courseGraphNodeMapper.selectActiveNodes();
        List<String> leafNodeIds = nodes.stream()
                .filter(n -> n.getParentId() != null)
                .map(CourseGraphNode::getId)
                .collect(Collectors.toList());

        // 3. 批量计算所有进度（3 条 SQL 替代 N*M*K 次查询）
        Map<String, int[]> batch = computeProgressBatch(studentIds, leafNodeIds);

        // 4. 知识点进度
        List<Map<String, Object>> kpProgress = new ArrayList<>();
        for (CourseGraphNode node : nodes) {
            if (node.getParentId() != null) {
                int totalComp = 0, totalMast = 0, count = 0;
                for (Long sid : studentIds) {
                    int[] p = getProgress(batch, sid, node.getId());
                    totalComp += p[0];
                    totalMast += p[1];
                    count++;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("name", node.getName());
                item.put("completion", count > 0 ? totalComp / count : 0);
                item.put("mastery", count > 0 ? totalMast / count : 0);
                kpProgress.add(item);
            }
        }
        result.put("kpProgress", kpProgress);

        // 5. 学情分段
        List<Map<String, Object>> segments = computeSegmentsBatch(studentIds, leafNodeIds, batch);
        result.put("segments", segments);

        // 6. 学生列表（完成率/掌握率）
        List<Map<String, Object>> studentList = computeStudentListBatch(studentIds, leafNodeIds, batch);
        result.put("students", studentList);

        // 7. 知识点热度排名
        List<Map<String, Object>> hotspotList = computeHotspotsBatch(studentIds, nodes, batch);
        result.put("hotspots", hotspotList);

        return result;
    }

    @Override
    public Map<String, Object> getCompare(Long classA, Long classB) {
        Map<String, Object> result = new HashMap<>();

        List<Long> studentsA = getStudentIdsByClass(classA);
        List<Long> studentsB = getStudentIdsByClass(classB);

        List<CourseGraphNode> nodes = courseGraphNodeMapper.selectActiveNodes();
        List<String> leafNodeIds = nodes.stream()
                .filter(n -> n.getParentId() != null)
                .map(CourseGraphNode::getId)
                .collect(Collectors.toList());

        // 批量计算两个班级的进度
        List<Long> allStudents = new ArrayList<>(studentsA);
        allStudents.addAll(studentsB);
        Map<String, int[]> batch = computeProgressBatch(allStudents, leafNodeIds);

        // 班级A进度
        int totalCompA = 0, totalMastA = 0, countA = 0;
        for (Long sid : studentsA) {
            int sComp = 0, sMast = 0, sCount = 0;
            for (String nodeId : leafNodeIds) {
                int[] p = getProgress(batch, sid, nodeId);
                sComp += p[0]; sMast += p[1]; sCount++;
            }
            if (sCount > 0) { totalCompA += sComp / sCount; totalMastA += sMast / sCount; countA++; }
        }

        // 班级B进度
        int totalCompB = 0, totalMastB = 0, countB = 0;
        for (Long sid : studentsB) {
            int sComp = 0, sMast = 0, sCount = 0;
            for (String nodeId : leafNodeIds) {
                int[] p = getProgress(batch, sid, nodeId);
                sComp += p[0]; sMast += p[1]; sCount++;
            }
            if (sCount > 0) { totalCompB += sComp / sCount; totalMastB += sMast / sCount; countB++; }
        }

        SysClass clsA = sysClassMapper.selectById(classA);
        SysClass clsB = sysClassMapper.selectById(classB);

        List<Map<String, Object>> classCompare = new ArrayList<>();
        Map<String, Object> itemA = new HashMap<>();
        itemA.put("name", clsA != null ? clsA.getName() : "班级A");
        itemA.put("completion", countA > 0 ? totalCompA / countA : 0);
        itemA.put("mastery", countA > 0 ? totalMastA / countA : 0);
        classCompare.add(itemA);

        Map<String, Object> itemB = new HashMap<>();
        itemB.put("name", clsB != null ? clsB.getName() : "班级B");
        itemB.put("completion", countB > 0 ? totalCompB / countB : 0);
        itemB.put("mastery", countB > 0 ? totalMastB / countB : 0);
        classCompare.add(itemB);

        result.put("classes", classCompare);

        // 分数段分布
        result.put("ranges", computeRangeDistributionBatch(studentsA, studentsB, leafNodeIds, batch));

        return result;
    }

    @Override
    public Map<String, Object> getStudentProfile(Long teacherId, Long classId) {
        Map<String, Object> result = new HashMap<>();
        List<Long> studentIds = getStudentIdsByClass(classId);

        List<CourseGraphNode> nodes = courseGraphNodeMapper.selectActiveNodes();
        List<String> leafNodeIds = nodes.stream()
                .filter(n -> n.getParentId() != null)
                .map(CourseGraphNode::getId)
                .collect(Collectors.toList());

        // 批量计算进度
        Map<String, int[]> batch = computeProgressBatch(studentIds, leafNodeIds);

        List<Map<String, Object>> students = new ArrayList<>();
        for (Long sid : studentIds) {
            User student = userMapper.selectById(sid);
            if (student == null) continue;

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", String.valueOf(sid));
            profile.put("name", student.getUserName());
            profile.put("avatar", student.getUserAvatar());

            // 计算该学生的整体完成率和掌握率
            int totalCompletion = 0;
            int totalMastery = 0;
            int count = 0;
            for (String nodeId : leafNodeIds) {
                int[] p = batch.get(sid + "_" + nodeId);
                if (p == null) {
                    continue;
                }
                totalCompletion += p[0];
                totalMastery += p[1];
                count++;
            }
            int completionRate = count > 0 ? totalCompletion / count : 0;
            int masteryRate = count > 0 ? totalMastery / count : 0;
            HomeworkFallbackStats homeworkFallback = computeHomeworkFallbackStats(sid, classId);
            if (count == 0 || count < 3) {
                completionRate = Math.max(completionRate, homeworkFallback.completionRate);
                masteryRate = Math.max(masteryRate, homeworkFallback.masteryRate);
            }
            profile.put("completionRate", completionRate);
            profile.put("masteryRate", masteryRate);

            // 雷达图数据（6维能力，从真实提交数据推导）
            profile.put("radar", computeStudentRadar(sid, classId, completionRate, masteryRate, homeworkFallback));

            // 学习日历（90天，从提交时间戳推导）
            profile.put("studyDays", computeStudyCalendar(sid));

            students.add(profile);
        }
        result.put("students", students);
        return result;
    }

    @Override
    public Map<String, Object> getBuildStats() {
        Map<String, Object> result = new HashMap<>();
        List<CourseGraphNode> nodes = courseGraphNodeMapper.selectActiveNodes();

        int total = nodes.size();
        int linked = 0;
        int tagged = 0;
        int crossLinked = 0;

        for (CourseGraphNode node : nodes) {
            List<CourseGraphNodeActivity> activities = courseGraphNodeActivityMapper.selectActiveByNodeId(node.getId());
            if (!activities.isEmpty()) linked++;
            if (node.getResourceTypes() != null && !node.getResourceTypes().isEmpty()) tagged++;
            Set<String> types = activities.stream().map(CourseGraphNodeActivity::getActivityType).collect(Collectors.toSet());
            if (types.size() >= 2) crossLinked++;
        }

        Map<String, Object> build = new HashMap<>();
        build.put("total", total);
        build.put("linked", linked);
        build.put("tagged", tagged);
        build.put("unlinked", total - linked);
        build.put("crossLinked", crossLinked);
        result.put("build", build);

        // 资源统计（按类型聚合）
        Map<String, Integer> resourceCounts = new HashMap<>();
        for (CourseGraphNode node : nodes) {
            List<CourseGraphNodeActivity> acts = courseGraphNodeActivityMapper.selectActiveByNodeId(node.getId());
            for (CourseGraphNodeActivity act : acts) {
                String type = act.getActivityType();
                resourceCounts.merge(type, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> resources = new ArrayList<>();
        String[][] typeMap = { {"homework", "关联作业", "FormOutlined"}, {"practice", "关联练习", "FormOutlined"},
                {"coding", "编程题目", "CodeOutlined"} };
        for (String[] tm : typeMap) {
            Map<String, Object> r = new HashMap<>();
            r.put("type", tm[0]);
            r.put("count", resourceCounts.getOrDefault(tm[0], 0));
            r.put("label", tm[1]);
            r.put("icon", tm[2]);
            resources.add(r);
        }
        result.put("resources", resources);

        // 知识点属性列表
        List<Map<String, Object>> attributes = new ArrayList<>();
        for (CourseGraphNode node : nodes) {
            if (node.getParentId() == null) continue; // 跳过根节点
            Map<String, Object> attr = new HashMap<>();
            attr.put("name", node.getName());
            attr.put("difficulty", node.getDifficulty());
            attr.put("importance", node.getIsKeyPoint() != null && node.getIsKeyPoint() == 1 ? "核心" : "重要");
            List<String> tags = new ArrayList<>();
            if (node.getCategory() != null) tags.add(node.getCategory());
            attr.put("tags", tags);
            attributes.add(attr);
        }
        result.put("attributes", attributes);

        // 收集所有不重复 category 作为可用筛选标签
        List<String> availableTags = nodes.stream()
                .map(CourseGraphNode::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        result.put("availableTags", availableTags);

        return result;
    }

    @Override
    public Map<String, List<Map<String, Object>>> getActivityCandidates(Long teacherId, Long courseId) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        // 查询教师的作业
        QueryWrapper<HomeworkAssignment> hwQuery = new QueryWrapper<>();
        hwQuery.eq("teacherId", teacherId).eq("status", "published");
        if (courseId != null) hwQuery.eq("courseId", courseId);
        List<HomeworkAssignment> assignments = homeworkAssignmentMapper.selectList(hwQuery);
        List<Map<String, Object>> homeworkList = new ArrayList<>();
        for (HomeworkAssignment hw : assignments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", hw.getId());
            item.put("title", hw.getTitle());
            item.put("type", "homework");
            homeworkList.add(item);
        }
        result.put("homework", homeworkList);

        // 查询教师的编程题
        QueryWrapper<CodingProblem> cpQuery = new QueryWrapper<>();
        cpQuery.eq("creator_id", teacherId);
        if (courseId != null) cpQuery.eq("course_id", courseId);
        List<CodingProblem> problems = codingProblemMapper.selectList(cpQuery);
        List<Map<String, Object>> codingList = new ArrayList<>();
        for (CodingProblem cp : problems) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cp.getId());
            item.put("title", cp.getTitle());
            item.put("type", "coding");
            codingList.add(item);
        }
        result.put("coding", codingList);

        return result;
    }

    // ═══════════════════════════════════════════════════
    //  批量进度计算（替代 N+1 查询）
    // ═══════════════════════════════════════════════════

    /**
     * 批量计算所有学生在所有节点的进度，返回 Map<(studentId_nodeId), {completion, mastery}>
     * 只需 3 条 SQL：1) 所有活动绑定 2) 所有作业提交 3) 所有编程提交
     */
    private Map<String, int[]> computeProgressBatch(List<Long> studentIds, List<String> nodeIds) {
        // 1. 一次查询所有活动绑定
        List<CourseGraphNodeActivity> allActivities = nodeIds.isEmpty()
                ? Collections.emptyList()
                : courseGraphNodeActivityMapper.selectActiveByNodeIds(nodeIds);

        // 按节点分组
        Map<String, List<CourseGraphNodeActivity>> activitiesByNode = allActivities.stream()
                .collect(Collectors.groupingBy(CourseGraphNodeActivity::getNodeId));

        // 收集所有活动ID，用于批量查分
        Set<Long> hwActivityIds = allActivities.stream()
                .filter(a -> "homework".equalsIgnoreCase(a.getActivityType()) || "practice".equalsIgnoreCase(a.getActivityType()))
                .map(CourseGraphNodeActivity::getActivityId)
                .collect(Collectors.toSet());
        Set<Long> codingActivityIds = allActivities.stream()
                .filter(a -> "coding".equalsIgnoreCase(a.getActivityType()))
                .map(CourseGraphNodeActivity::getActivityId)
                .collect(Collectors.toSet());

        // 2. 批量查询作业提交，构建 (studentId_assignmentId) -> 最高分 的映射
        Map<String, Integer> hwScoreMap = new HashMap<>();
        if (!hwActivityIds.isEmpty() && !studentIds.isEmpty()) {
            QueryWrapper<HomeworkSubmission> hwQuery = new QueryWrapper<>();
            hwQuery.in("studentId", studentIds)
                    .in("assignmentId", hwActivityIds)
                    .eq("submitStatus", "completed");
            List<HomeworkSubmission> hwSubs = homeworkSubmissionMapper.selectList(hwQuery);
            for (HomeworkSubmission sub : hwSubs) {
                if (sub.getTotalScore() != null) {
                    String key = sub.getStudentId() + "_" + sub.getAssignmentId();
                    hwScoreMap.merge(key, sub.getTotalScore(), Math::max);
                }
            }
        }

        // 3. 批量查询编程提交，构建 (studentId_problemId) -> 最高分 的映射
        Map<String, Integer> codingScoreMap = new HashMap<>();
        if (!codingActivityIds.isEmpty() && !studentIds.isEmpty()) {
            QueryWrapper<CodingSubmission> codingQuery = new QueryWrapper<>();
            codingQuery.in("student_id", studentIds)
                    .in("problem_id", codingActivityIds)
                    .eq("status", "judged");
            List<CodingSubmission> codingSubs = codingSubmissionMapper.selectList(codingQuery);
            for (CodingSubmission sub : codingSubs) {
                if (sub.getFinalScore() != null) {
                    String key = sub.getStudentId() + "_" + sub.getProblemId();
                    codingScoreMap.merge(key, sub.getFinalScore(), Math::max);
                }
            }
        }

        // 4. 内存中计算每个学生在每个节点的完成率和掌握率
        Map<String, int[]> progressMap = new HashMap<>();
        for (Long sid : studentIds) {
            for (String nodeId : nodeIds) {
                List<CourseGraphNodeActivity> nodeActs = activitiesByNode.getOrDefault(nodeId, Collections.emptyList());
                if (nodeActs.isEmpty()) continue;

                int completed = 0;
                int scoreSum = 0;
                int scoredCount = 0;

                for (CourseGraphNodeActivity act : nodeActs) {
                    String scoreKey = sid + "_" + act.getActivityId();
                    Integer score = null;
                    if ("homework".equalsIgnoreCase(act.getActivityType()) || "practice".equalsIgnoreCase(act.getActivityType())) {
                        score = hwScoreMap.get(scoreKey);
                    } else if ("coding".equalsIgnoreCase(act.getActivityType())) {
                        score = codingScoreMap.get(scoreKey);
                    }
                    if (score != null && score >= 0) {
                        completed++;
                        scoreSum += score;
                        scoredCount++;
                    }
                }

                int totalActs = nodeActs.size();
                int completion = totalActs > 0 ? (int) Math.round((double) completed / totalActs * 100) : 0;
                int mastery = scoredCount > 0 ? scoreSum / scoredCount : 0;

                progressMap.put(sid + "_" + nodeId, new int[]{completion, mastery});
            }
        }

        return progressMap;
    }

    /**
     * 获取学生在某节点的进度（从批量结果中取）
     */
    private int[] getProgress(Map<String, int[]> batch, Long studentId, String nodeId) {
        int[] p = batch.get(studentId + "_" + nodeId);
        return p != null ? p : new int[]{0, 0};
    }

    // ═══════════════════════════════════════════════════
    //  辅助方法
    // ═══════════════════════════════════════════════════

    private List<Map<String, Object>> computeHotspotsBatch(List<Long> studentIds, List<CourseGraphNode> nodes, Map<String, int[]> batch) {
        List<Map<String, Object>> hotspotList = new ArrayList<>();
        for (CourseGraphNode node : nodes) {
            if (node.getParentId() == null) continue;
            int strugglingCount = 0;
            for (Long sid : studentIds) {
                int[] p = getProgress(batch, sid, node.getId());
                if (p[1] > 0 && p[1] < 60) strugglingCount++;
            }
            int heatScore = Math.min(strugglingCount * 20, 100);
            if (heatScore > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", node.getName());
                item.put("heatScore", heatScore);
                item.put("questionCount", strugglingCount);
                hotspotList.add(item);
            }
        }
        hotspotList.sort((a, b) -> ((Integer) b.get("heatScore")).compareTo((Integer) a.get("heatScore")));
        return hotspotList.stream().limit(8).collect(Collectors.toList());
    }

    private List<Long> getStudentIdsByClass(Long classId) {
        if (classId == null) {
            // 默认返回所有学生
            QueryWrapper<User> query = new QueryWrapper<>();
            query.eq("userRole", "student");
            return userMapper.selectList(query).stream().map(User::getId).collect(Collectors.toList());
        }
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("class_id", classId).eq("userRole", "student");
        return userMapper.selectList(query).stream().map(User::getId).collect(Collectors.toList());
    }

    private List<Map<String, Object>> computeSegmentsBatch(List<Long> studentIds, List<String> nodeIds, Map<String, int[]> batch) {
        int excellent = 0, good = 0, medium = 0, poor = 0;
        for (Long sid : studentIds) {
            int totalMastery = 0, count = 0;
            for (String nodeId : nodeIds) {
                int[] p = getProgress(batch, sid, nodeId);
                if (p[1] > 0) { totalMastery += p[1]; count++; }
            }
            int avg = count > 0 ? totalMastery / count : 0;
            if (avg >= 90) excellent++;
            else if (avg >= 75) good++;
            else if (avg >= 60) medium++;
            else poor++;
        }
        List<Map<String, Object>> segments = new ArrayList<>();
        segments.add(createSegment("优秀 (90-100%)", excellent, "#10B981"));
        segments.add(createSegment("良好 (75-89%)", good, "#3B82F6"));
        segments.add(createSegment("中等 (60-74%)", medium, "#F59E0B"));
        segments.add(createSegment("待提升 (<60%)", poor, "#EF4444"));
        return segments;
    }

    private Map<String, Object> createSegment(String label, int count, String color) {
        Map<String, Object> s = new HashMap<>();
        s.put("label", label);
        s.put("count", count);
        s.put("color", color);
        return s;
    }

    private List<Map<String, Object>> computeStudentListBatch(List<Long> studentIds, List<String> nodeIds, Map<String, int[]> batch) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Long sid : studentIds) {
            User student = userMapper.selectById(sid);
            if (student == null) continue;
            int totalComp = 0, totalMast = 0, count = 0;
            for (String nodeId : nodeIds) {
                int[] p = getProgress(batch, sid, nodeId);
                totalComp += p[0]; totalMast += p[1]; count++;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("name", student.getUserName());
            item.put("completion", count > 0 ? totalComp / count : 0);
            item.put("mastery", count > 0 ? totalMast / count : 0);
            list.add(item);
        }
        list.sort((a, b) -> ((Integer) b.get("mastery")).compareTo((Integer) a.get("mastery")));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).put("rank", i + 1);
        }
        return list;
    }

    private List<Map<String, Object>> computeRangeDistributionBatch(List<Long> studentsA, List<Long> studentsB, List<String> nodeIds, Map<String, int[]> batch) {
        String[] ranges = {"0%-59%", "60%-69%", "70%-79%", "80%-89%", "90%-100%"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (String range : ranges) {
            Map<String, Object> item = new HashMap<>();
            item.put("range", range);
            item.put("classA", countInRangeBatch(studentsA, nodeIds, range, batch));
            item.put("classB", countInRangeBatch(studentsB, nodeIds, range, batch));
            result.add(item);
        }
        return result;
    }

    private int countInRangeBatch(List<Long> studentIds, List<String> nodeIds, String range, Map<String, int[]> batch) {
        int min = 0, max = 100;
        if (range.startsWith("0%")) { min = 0; max = 59; }
        else if (range.startsWith("60%")) { min = 60; max = 69; }
        else if (range.startsWith("70%")) { min = 70; max = 79; }
        else if (range.startsWith("80%")) { min = 80; max = 89; }
        else if (range.startsWith("90%")) { min = 90; max = 100; }

        int count = 0;
        for (Long sid : studentIds) {
            int totalMast = 0, c = 0;
            for (String nodeId : nodeIds) {
                int[] p = getProgress(batch, sid, nodeId);
                if (p[1] > 0) { totalMast += p[1]; c++; }
            }
            int avg = c > 0 ? totalMast / c : 0;
            if (avg >= min && avg <= max) count++;
        }
        return count;
    }

    private static class HomeworkFallbackStats {
        int completionRate;
        int masteryRate;
        int averageScoreRate;
    }

    private HomeworkFallbackStats computeHomeworkFallbackStats(Long studentId, Long classId) {
        HomeworkFallbackStats stats = new HomeworkFallbackStats();

        QueryWrapper<HomeworkAssignment> assignmentQuery = new QueryWrapper<>();
        assignmentQuery.eq("status", "published")
                .in("assignmentType", Arrays.asList("homework", "chapter_practice"));
        if (classId != null) {
            assignmentQuery.eq("classId", classId);
        }
        List<HomeworkAssignment> assignments = homeworkAssignmentMapper.selectList(assignmentQuery);

        List<Long> assignmentIds = assignments.stream()
                .map(HomeworkAssignment::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        QueryWrapper<HomeworkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.eq("studentId", studentId).eq("submitStatus", "completed");
        if (!assignmentIds.isEmpty()) {
            submissionQuery.in("assignmentId", assignmentIds);
        } else if (classId != null) {
            submissionQuery.eq("classId", classId);
        }
        List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectList(submissionQuery);

        Set<Long> completedAssignmentIds = submissions.stream()
                .map(HomeworkSubmission::getAssignmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!assignments.isEmpty()) {
            stats.completionRate = (int) Math.round(completedAssignmentIds.size() * 100.0 / assignments.size());
        }

        Map<Long, Integer> fullScoreByAssignment = assignments.stream()
                .filter(a -> a.getId() != null && a.getTotalScore() != null && a.getTotalScore() > 0)
                .collect(Collectors.toMap(HomeworkAssignment::getId, HomeworkAssignment::getTotalScore, (a, b) -> a));

        Map<Long, Integer> bestScoreRateByAssignment = new HashMap<>();
        for (HomeworkSubmission sub : submissions) {
            if (sub.getAssignmentId() == null || sub.getTotalScore() == null) {
                continue;
            }
            Integer fullScore = fullScoreByAssignment.get(sub.getAssignmentId());
            int scoreRate = fullScore != null && fullScore > 0
                    ? (int) Math.round(sub.getTotalScore() * 100.0 / fullScore)
                    : sub.getTotalScore();
            scoreRate = Math.max(0, Math.min(100, scoreRate));
            bestScoreRateByAssignment.merge(sub.getAssignmentId(), scoreRate, Math::max);
        }

        if (!bestScoreRateByAssignment.isEmpty()) {
            int sum = bestScoreRateByAssignment.values().stream().mapToInt(Integer::intValue).sum();
            stats.masteryRate = Math.round((float) sum / bestScoreRateByAssignment.size());
            stats.averageScoreRate = stats.masteryRate;
        }

        return stats;
    }

    /**
     * 从真实提交数据推导学生6维能力雷达图
     */
    private List<Map<String, Object>> computeStudentRadar(Long studentId, Long classId, int completionRate, int masteryRate, HomeworkFallbackStats homeworkFallback) {
        // 查询作业提交（客观题/主观题分数）
        QueryWrapper<HomeworkSubmission> hwQuery = new QueryWrapper<>();
        hwQuery.eq("studentId", studentId).eq("submitStatus", "completed");
        if (classId != null) {
            hwQuery.eq("classId", classId);
        }
        List<HomeworkSubmission> hwSubs = homeworkSubmissionMapper.selectList(hwQuery);

        int objScoreSum = 0, objCount = 0;
        int subjScoreSum = 0, subjCount = 0;
        for (HomeworkSubmission sub : hwSubs) {
            if (sub.getObjectiveScore() != null && sub.getTotalScore() != null && sub.getTotalScore() > 0) {
                objScoreSum += (int) Math.round((double) sub.getObjectiveScore() / sub.getTotalScore() * 100);
                objCount++;
            }
            if (sub.getSubjectiveScore() != null && sub.getTotalScore() != null && sub.getTotalScore() > 0) {
                subjScoreSum += (int) Math.round((double) sub.getSubjectiveScore() / sub.getTotalScore() * 100);
                subjCount++;
            }
        }
        int theoryBase = objCount > 0 ? objScoreSum / objCount : 0;
        int practiceBase = subjCount > 0 ? subjScoreSum / subjCount : 0;
        if (theoryBase == 0 && homeworkFallback.averageScoreRate > 0) {
            theoryBase = homeworkFallback.averageScoreRate;
        }
        if (practiceBase == 0 && homeworkFallback.averageScoreRate > 0) {
            practiceBase = homeworkFallback.averageScoreRate;
        }

        // 查询编程题提交
        QueryWrapper<CodingSubmission> codingQuery = new QueryWrapper<>();
        codingQuery.eq("student_id", studentId).eq("status", "judged");
        List<CodingSubmission> codingSubs = codingSubmissionMapper.selectList(codingQuery);

        int codingScoreSum = 0, codingCount = 0;
        for (CodingSubmission sub : codingSubs) {
            if (sub.getFinalScore() != null) {
                codingScoreSum += sub.getFinalScore();
                codingCount++;
            }
        }
        int codingBase = codingCount > 0 ? codingScoreSum / codingCount : 0;

        // 持续度：近14天有提交的天数占比
        LocalDate today = LocalDate.now();
        Set<LocalDate> activeDays = new HashSet<>();
        for (HomeworkSubmission sub : hwSubs) {
            if (sub.getSubmitTime() != null) {
                LocalDate d = sub.getSubmitTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (!d.isBefore(today.minusDays(14))) activeDays.add(d);
            }
        }
        for (CodingSubmission sub : codingSubs) {
            if (sub.getCreateTime() != null) {
                LocalDate d = sub.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (!d.isBefore(today.minusDays(14))) activeDays.add(d);
            }
        }
        int continuity = (int) Math.round((double) activeDays.size() / 14 * 100);

        // 构建6维雷达
        String[] dimensions = {"理论基础", "实践能力", "编程能力", "完成度", "掌握度", "持续度"};
        int[] values = {theoryBase, practiceBase, codingBase, completionRate, masteryRate, continuity};

        List<Map<String, Object>> radar = new ArrayList<>();
        for (int i = 0; i < dimensions.length; i++) {
            Map<String, Object> r = new HashMap<>();
            r.put("indicator", dimensions[i]);
            r.put("value", Math.min(100, values[i]));
            r.put("max", 100);
            radar.add(r);
        }
        return radar;
    }

    /**
     * 从提交时间戳推导学生近90天学习日历
     */
    private List<Map<String, Object>> computeStudyCalendar(Long studentId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(89);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 查询近90天作业提交
        QueryWrapper<HomeworkSubmission> hwQuery = new QueryWrapper<>();
        hwQuery.eq("studentId", studentId).eq("submitStatus", "completed");
        List<HomeworkSubmission> hwSubs = homeworkSubmissionMapper.selectList(hwQuery);

        // 查询近90天编程提交
        QueryWrapper<CodingSubmission> codingQuery = new QueryWrapper<>();
        codingQuery.eq("student_id", studentId).eq("status", "judged");
        List<CodingSubmission> codingSubs = codingSubmissionMapper.selectList(codingQuery);

        // 按日期统计提交数
        Map<LocalDate, Integer> dailySubmissions = new HashMap<>();
        for (HomeworkSubmission sub : hwSubs) {
            if (sub.getSubmitTime() != null) {
                LocalDate d = sub.getSubmitTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (!d.isBefore(startDate) && !d.isAfter(today)) {
                    dailySubmissions.merge(d, 1, Integer::sum);
                }
            }
        }
        for (CodingSubmission sub : codingSubs) {
            if (sub.getCreateTime() != null) {
                LocalDate d = sub.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (!d.isBefore(startDate) && !d.isAfter(today)) {
                    dailySubmissions.merge(d, 1, Integer::sum);
                }
            }
        }

        // 构建90天日历
        List<Map<String, Object>> studyDays = new ArrayList<>();
        for (int i = 0; i <= 89; i++) {
            LocalDate d = startDate.plusDays(i);
            int submissions = dailySubmissions.getOrDefault(d, 0);
            Map<String, Object> day = new HashMap<>();
            day.put("date", d.format(fmt));
            day.put("minutes", Math.min(submissions * 20, 180));
            day.put("completed", submissions);
            studyDays.add(day);
        }
        return studyDays;
    }
}
