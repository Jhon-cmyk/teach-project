package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.CodingProblemAddRequest;
import com.ruyi.teach.model.dto.CodingProblemPublishRequest;
import com.ruyi.teach.model.dto.CodingProblemUpdateRequest;
import com.ruyi.teach.model.entity.*;
import com.ruyi.teach.model.vo.CodingProblemVO;
import com.ruyi.teach.model.vo.CodingSubmissionVO;
import com.ruyi.teach.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/coding/problem")
@Tag(name = "编程题管理")
public class CodingProblemController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private CodingProblemService problemService;

    @Resource
    private CodingProblemTemplateService templateService;

    @Resource
    private CodingTestCaseService testCaseService;

    @Resource
    private CodingProblemPublishService publishService;

    @Resource
    private CodingSubmissionService submissionService;

    @Resource
    private UserService userService;

    @Resource
    private CourseService courseService;

    @Resource
    private RoleAuthorizationService roleAuthorizationService;

    @Resource
    private com.ruyi.teach.mapper.SysClassMapper sysClassMapper;

    @Resource
    private com.ruyi.teach.mapper.CodingProblemTemplateMapper templateMapper;

    @Operation(summary = "教师创建编程题")
    @PostMapping("/add")
    public BaseResponse<Long> addProblem(@Valid @RequestBody CodingProblemAddRequest req,
                                         HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可创建编程题");
        }
        if (req.getCourseId() != null) {
            requireOwnedCourse(req.getCourseId(), loginUser);
        }
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
        }
        if (req.getDescription() == null || req.getDescription().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目描述不能为空");
        }
        if (req.getLanguages() == null || req.getLanguages().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "至少选择一种语言");
        }
        if (req.getTestCases() != null && !req.getTestCases().isEmpty()) {
            for (int i = 0; i < req.getTestCases().size(); i++) {
                CodingProblemAddRequest.CodingTestCaseItem tc = req.getTestCases().get(i);
                if (tc.getExpectedOutput() == null || tc.getExpectedOutput().trim().isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "测试用例 " + (i + 1) + " 的期望输出不能为空");
                }
            }
            boolean hasNonSample = req.getTestCases().stream().anyMatch(tc -> tc.getIsSample() == null || tc.getIsSample() == 0);
            if (!hasNonSample) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "至少需要一个非样例测试用例用于判分");
            }
        }

        // 校验同一教师下是否已存在相同标题
        LambdaQueryWrapper<CodingProblem> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(CodingProblem::getCreatorId, loginUser.getId())
                .eq(CodingProblem::getTitle, req.getTitle().trim())
                .eq(CodingProblem::getIsDelete, 0);
        if (problemService.count(dupWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该标题的题目已存在，请勿重复创建");
        }

        // 1. 保存题目
        CodingProblem problem = new CodingProblem();
        problem.setTitle(req.getTitle());
        problem.setDescription(req.getDescription());
        problem.setDifficulty(req.getDifficulty() != null ? req.getDifficulty() : "medium");
        try {
            problem.setLanguages(objectMapper.writeValueAsString(req.getLanguages()));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "语言列表格式错误");
        }
        problem.setTimeLimitMs(req.getTimeLimitMs() != null ? req.getTimeLimitMs() : 5000);
        problem.setMemoryLimitKb(req.getMemoryLimitKb() != null ? req.getMemoryLimitKb() : 262144);
        problem.setCourseId(req.getCourseId());
        problem.setSemesterLabel(normalizeSemesterLabel(req.getSemesterLabel()));
        problem.setCreatorId(loginUser.getId());
        problem.setIsPublic(0);
        problemService.save(problem);

        Long problemId = problem.getId();

        // 2. 保存多语言模板
        if (req.getTemplates() != null) {
            for (CodingProblemAddRequest.CodingTemplateItem item : req.getTemplates()) {
                CodingProblemTemplate tpl = new CodingProblemTemplate();
                tpl.setProblemId(problemId);
                tpl.setLanguage(item.getLanguage());
                tpl.setStarterCode(item.getStarterCode());
                tpl.setReferenceSolution(item.getReferenceSolution());
                templateService.save(tpl);
            }
        }

        // 3. 保存测试用例
        if (req.getTestCases() != null) {
            int order = 0;
            for (CodingProblemAddRequest.CodingTestCaseItem item : req.getTestCases()) {
                CodingTestCase tc = new CodingTestCase();
                tc.setProblemId(problemId);
                tc.setInput(item.getInput());
                tc.setExpectedOutput(item.getExpectedOutput());
                tc.setIsSample(item.getIsSample() != null ? item.getIsSample() : 0);
                tc.setScore(item.getScore() != null ? item.getScore() : 0);
                tc.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : order++);
                testCaseService.save(tc);
            }
        }

        return ResultUtils.success(problemId);
    }

    @Operation(summary = "教师查询编程题列表")
    @GetMapping("/teacher/list")
    public BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<CodingProblemVO>> listTeacherProblems(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String semesterLabel,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可查看");
        }

        LambdaQueryWrapper<CodingProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodingProblem::getCreatorId, loginUser.getId())
                .eq(CodingProblem::getIsDelete, 0);
        if (courseId != null) {
            wrapper.eq(CodingProblem::getCourseId, courseId);
        }
        if (semesterLabel != null && !semesterLabel.trim().isEmpty()) {
            wrapper.eq(CodingProblem::getSemesterLabel, semesterLabel.trim());
        }
        wrapper.orderByDesc(CodingProblem::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CodingProblem> page =
                problemService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size), wrapper);
        List<CodingProblemVO> voList = page.getRecords().stream().map(this::toSimpleVO).collect(Collectors.toList());

        // 补全发布班级数 / 提交人次 / 已发布班级名
        if (!voList.isEmpty()) {
            List<Long> problemIds = voList.stream().map(CodingProblemVO::getId).collect(Collectors.toList());

            LambdaQueryWrapper<CodingProblemPublish> pubWrapper = new LambdaQueryWrapper<>();
            pubWrapper.in(CodingProblemPublish::getProblemId, problemIds)
                    .eq(CodingProblemPublish::getIsDelete, 0);
            List<CodingProblemPublish> pubList = publishService.list(pubWrapper);
            Map<Long, Long> pubCountMap = pubList.stream()
                    .collect(Collectors.groupingBy(CodingProblemPublish::getProblemId, Collectors.counting()));

            Set<Long> classIds = pubList.stream()
                    .map(CodingProblemPublish::getClassId)
                    .collect(Collectors.toSet());
            Map<Long, String> classNameMap = new HashMap<>();
            if (!classIds.isEmpty()) {
                List<SysClass> classes = sysClassMapper.selectBatchIds(classIds);
                for (SysClass c : classes) {
                    classNameMap.put(c.getId(), c.getName());
                }
            }
            Map<Long, List<String>> pubClassMap = pubList.stream()
                    .collect(Collectors.groupingBy(
                            CodingProblemPublish::getProblemId,
                            Collectors.mapping(p -> classNameMap.getOrDefault(p.getClassId(), "班级" + p.getClassId()),
                                    Collectors.toList())));

            LambdaQueryWrapper<CodingSubmission> subWrapper = new LambdaQueryWrapper<>();
            subWrapper.in(CodingSubmission::getProblemId, problemIds)
                    .eq(CodingSubmission::getIsDelete, 0);
            Map<Long, Integer> subCountMap = submissionService.list(subWrapper).stream()
                    .collect(Collectors.groupingBy(
                            CodingSubmission::getProblemId,
                            Collectors.mapping(CodingSubmission::getStudentId, Collectors.collectingAndThen(
                                    Collectors.toSet(), Set::size
                            ))
                    ));

            for (CodingProblemVO vo : voList) {
                vo.setPublishCount(pubCountMap.getOrDefault(vo.getId(), 0L).intValue());
                vo.setSubmissionCount(subCountMap.getOrDefault(vo.getId(), 0));
                vo.setPublishedClasses(pubClassMap.getOrDefault(vo.getId(), Collections.emptyList()));
            }
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CodingProblemVO> resultPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);
        return ResultUtils.success(resultPage);
    }

    @Operation(summary = "教师查询编程题完整详情（含所有测试用例和参考解）")
    @GetMapping("/teacher/detail")
    public BaseResponse<CodingProblemVO> getTeacherProblemDetail(@RequestParam Long problemId,
                                                                  HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可查看完整详情");
        }

        CodingProblem problem = problemService.getById(problemId);
        if (problem == null || (problem.getIsDelete() != null && problem.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "编程题不存在");
        }
        if (!problem.getCreatorId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能查看自己创建的题目");
        }

        CodingProblemVO vo = toSimpleVO(problem);

        // 所有测试用例
        LambdaQueryWrapper<CodingTestCase> tcWrapper = new LambdaQueryWrapper<>();
        tcWrapper.eq(CodingTestCase::getProblemId, problemId)
                .eq(CodingTestCase::getIsDelete, 0)
                .orderByAsc(CodingTestCase::getSortOrder);
        List<CodingTestCase> allCases = testCaseService.list(tcWrapper);
        vo.setAllTestCases(allCases.stream().map(tc -> {
            CodingProblemVO.CodingTestCaseVO tcVO = new CodingProblemVO.CodingTestCaseVO();
            tcVO.setId(tc.getId());
            tcVO.setInput(tc.getInput());
            tcVO.setExpectedOutput(tc.getExpectedOutput());
            tcVO.setIsSample(tc.getIsSample());
            tcVO.setScore(tc.getScore());
            tcVO.setSortOrder(tc.getSortOrder());
            return tcVO;
        }).collect(Collectors.toList()));

        // 模板（含参考解）
        LambdaQueryWrapper<CodingProblemTemplate> tplWrapper = new LambdaQueryWrapper<>();
        tplWrapper.eq(CodingProblemTemplate::getProblemId, problemId)
                .eq(CodingProblemTemplate::getIsDelete, 0);
        List<CodingProblemTemplate> templates = templateService.list(tplWrapper);
        vo.setTemplatesWithSolution(templates.stream().map(tpl -> {
            CodingProblemVO.CodingTemplateVO tplVO = new CodingProblemVO.CodingTemplateVO();
            tplVO.setLanguage(tpl.getLanguage());
            tplVO.setStarterCode(tpl.getStarterCode());
            tplVO.setReferenceSolution(tpl.getReferenceSolution());
            return tplVO;
        }).collect(Collectors.toList()));

        return ResultUtils.success(vo);
    }

    @Operation(summary = "教师更新编程题")
    @PostMapping("/update")
    @Transactional
    public BaseResponse<Boolean> updateProblem(@Valid @RequestBody CodingProblemUpdateRequest req,
                                                HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可更新");
        }
        if (req.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不能为空");
        }

        CodingProblem existing = problemService.getById(req.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "编程题不存在");
        }
        if (!existing.getCreatorId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能更新自己创建的题目");
        }
        if (req.getCourseId() != null) {
            requireOwnedCourse(req.getCourseId(), loginUser);
        }

        if (req.getTestCases() != null && !req.getTestCases().isEmpty()) {
            for (int i = 0; i < req.getTestCases().size(); i++) {
                CodingProblemAddRequest.CodingTestCaseItem tc = req.getTestCases().get(i);
                if (tc.getExpectedOutput() == null || tc.getExpectedOutput().trim().isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "测试用例 " + (i + 1) + " 的期望输出不能为空");
                }
            }
            boolean hasNonSample = req.getTestCases().stream().anyMatch(tc -> tc.getIsSample() == null || tc.getIsSample() == 0);
            if (!hasNonSample) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "至少需要一个非样例测试用例用于判分");
            }
        }

        if (req.getTitle() != null && !req.getTitle().trim().isEmpty()) {
            existing.setTitle(req.getTitle());
        }
        if (req.getDescription() != null && !req.getDescription().trim().isEmpty()) {
            existing.setDescription(req.getDescription());
        }
        if (req.getDifficulty() != null) {
            existing.setDifficulty(req.getDifficulty());
        }
        if (req.getLanguages() != null && !req.getLanguages().isEmpty()) {
            try {
                existing.setLanguages(objectMapper.writeValueAsString(req.getLanguages()));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "语言列表格式错误");
            }
        }
        if (req.getTimeLimitMs() != null) {
            existing.setTimeLimitMs(req.getTimeLimitMs());
        }
        if (req.getMemoryLimitKb() != null) {
            existing.setMemoryLimitKb(req.getMemoryLimitKb());
        }
        if (req.getCourseId() != null) {
            existing.setCourseId(req.getCourseId());
        }
        existing.setSemesterLabel(normalizeSemesterLabel(req.getSemesterLabel()));
        problemService.updateById(existing);

        // 模板：若传入则物理删除旧记录后重新插入（避免唯一索引 uk_problem_lang 冲突）
        if (req.getTemplates() != null) {
            templateMapper.deleteByProblemIdPhysical(req.getId());
            for (CodingProblemAddRequest.CodingTemplateItem item : req.getTemplates()) {
                CodingProblemTemplate tpl = new CodingProblemTemplate();
                tpl.setProblemId(req.getId());
                tpl.setLanguage(item.getLanguage());
                tpl.setStarterCode(item.getStarterCode());
                tpl.setReferenceSolution(item.getReferenceSolution());
                templateService.save(tpl);
            }
        }

        // 测试用例：若传入则逻辑删除旧记录+新增
        if (req.getTestCases() != null) {
            LambdaQueryWrapper<CodingTestCase> tcOld = new LambdaQueryWrapper<>();
            tcOld.eq(CodingTestCase::getProblemId, req.getId())
                    .eq(CodingTestCase::getIsDelete, 0);
            List<CodingTestCase> oldCases = testCaseService.list(tcOld);
            for (CodingTestCase tc : oldCases) {
                tc.setIsDelete(1);
                testCaseService.updateById(tc);
            }
            int order = 0;
            for (CodingProblemAddRequest.CodingTestCaseItem item : req.getTestCases()) {
                CodingTestCase tc = new CodingTestCase();
                tc.setProblemId(req.getId());
                tc.setInput(item.getInput());
                tc.setExpectedOutput(item.getExpectedOutput());
                tc.setIsSample(item.getIsSample() != null ? item.getIsSample() : 0);
                tc.setScore(item.getScore() != null ? item.getScore() : 0);
                tc.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : order++);
                testCaseService.save(tc);
            }
        }

        return ResultUtils.success(true);
    }

    @Operation(summary = "教师查询某题的所有学生提交")
    @GetMapping("/teacher/submissions")
    public BaseResponse<List<CodingSubmissionVO>> listProblemSubmissions(@RequestParam Long problemId,
                                                                          HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可查看");
        }
        CodingProblem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "编程题不存在");
        }
        if (!problem.getCreatorId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能查看自己创建的题目");
        }

        LambdaQueryWrapper<CodingSubmission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(CodingSubmission::getProblemId, problemId)
                .eq(CodingSubmission::getIsDelete, 0)
                .orderByDesc(CodingSubmission::getCreateTime);
        List<CodingSubmission> submissions = submissionService.list(subWrapper);

        // 每个学生只保留最新一条提交
        List<CodingSubmission> uniqueSubmissions = new ArrayList<>();
        Set<Long> seenStudents = new HashSet<>();
        for (CodingSubmission s : submissions) {
            if (s.getStudentId() != null && !seenStudents.contains(s.getStudentId())) {
                seenStudents.add(s.getStudentId());
                uniqueSubmissions.add(s);
            }
        }

        Map<Long, String> studentNameMap = new HashMap<>();
        if (!uniqueSubmissions.isEmpty()) {
            Set<Long> studentIds = uniqueSubmissions.stream()
                    .map(CodingSubmission::getStudentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!studentIds.isEmpty()) {
                List<User> users = userService.listByIds(studentIds);
                for (User u : users) {
                    String name = u.getUserName() != null ? u.getUserName() : ("用户" + u.getId());
                    studentNameMap.put(u.getId(), name);
                }
            }
        }

        List<CodingSubmissionVO> voList = uniqueSubmissions.stream().map(s -> {
            CodingSubmissionVO vo = new CodingSubmissionVO();
            vo.setId(s.getId());
            vo.setProblemId(s.getProblemId());
            vo.setStudentId(s.getStudentId());
            vo.setStudentName(studentNameMap.getOrDefault(s.getStudentId(), "学生" + s.getStudentId()));
            vo.setLanguage(s.getLanguage());
            vo.setStatus(s.getStatus());
            vo.setPassedCount(s.getPassedCount());
            vo.setTotalCount(s.getTotalCount());
            vo.setTestScore(s.getTestScore());
            vo.setAiScore(s.getAiScore());
            vo.setFinalScore(s.getFinalScore());
            vo.setAiReviewMd(s.getAiReviewMd());
            vo.setRuntimeMs(s.getRuntimeMs());
            vo.setMemoryKb(s.getMemoryKb());
            vo.setCreateTime(s.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        return ResultUtils.success(voList);
    }

    @Operation(summary = "获取编程题详情")
    @GetMapping("/detail")
    public BaseResponse<CodingProblemVO> getProblemDetail(@RequestParam Long problemId,
                                                           HttpServletRequest request) {
        User loginUser = getLoginUser(request); // 仅校验登录

        CodingProblem problem = problemService.getById(problemId);
        if (problem == null || (problem.getIsDelete() != null && problem.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "编程题不存在");
        }
        CodingProblemPublish publish = requireStudentPublish(problemId, loginUser);

        CodingProblemVO vo = toSimpleVO(problem);
        vo.setDeadline(publish.getDeadline());

        // 样例用例
        LambdaQueryWrapper<CodingTestCase> tcWrapper = new LambdaQueryWrapper<>();
        tcWrapper.eq(CodingTestCase::getProblemId, problemId)
                .eq(CodingTestCase::getIsSample, 1)
                .eq(CodingTestCase::getIsDelete, 0)
                .orderByAsc(CodingTestCase::getSortOrder);
        List<CodingTestCase> sampleCases = testCaseService.list(tcWrapper);
        vo.setSampleTestCases(sampleCases.stream().map(tc -> {
            CodingProblemVO.CodingTestCaseVO tcVO = new CodingProblemVO.CodingTestCaseVO();
            tcVO.setId(tc.getId());
            tcVO.setInput(tc.getInput());
            tcVO.setExpectedOutput(tc.getExpectedOutput());
            tcVO.setSortOrder(tc.getSortOrder());
            return tcVO;
        }).collect(Collectors.toList()));

        // 模板
        LambdaQueryWrapper<CodingProblemTemplate> tplWrapper = new LambdaQueryWrapper<>();
        tplWrapper.eq(CodingProblemTemplate::getProblemId, problemId)
                .eq(CodingProblemTemplate::getIsDelete, 0);
        List<CodingProblemTemplate> templates = templateService.list(tplWrapper);
        vo.setTemplates(templates.stream().map(tpl -> {
            CodingProblemVO.CodingTemplateVO tplVO = new CodingProblemVO.CodingTemplateVO();
            tplVO.setLanguage(tpl.getLanguage());
            tplVO.setStarterCode(tpl.getStarterCode());
            return tplVO;
        }).collect(Collectors.toList()));

        return ResultUtils.success(vo);
    }

    @Operation(summary = "教师删除编程题")
    @PostMapping("/delete/{problemId}")
    public BaseResponse<Boolean> deleteProblem(@PathVariable Long problemId,
                                                HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可操作");
        }
        CodingProblem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "编程题不存在");
        }
        if (!problem.getCreatorId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能删除自己创建的题目");
        }
        return ResultUtils.success(problemService.removeById(problemId));
    }

    @Operation(summary = "教师发布编程题给班级")
    @PostMapping("/publish")
    public BaseResponse<String> publishProblem(@Valid @RequestBody CodingProblemPublishRequest req,
                                                 HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可发布");
        }
        if (req.getProblemId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不能为空");
        }
        if (req.getClassIds() == null || req.getClassIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "至少选择一个班级");
        }

        CodingProblem problem = problemService.getById(req.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "编程题不存在");
        }
        roleAuthorizationService.requireOwner(loginUser, problem.getCreatorId(), "编程题");

        Set<Long> allowedClassIds = sysClassMapper.selectMyClasses(loginUser.getId()).stream()
                .map(SysClass::getId)
                .collect(Collectors.toSet());
        if (!allowedClassIds.containsAll(req.getClassIds())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能向本人授课班级发布编程题");
        }

        int skipped = 0;
        for (Long classId : req.getClassIds()) {
            LambdaQueryWrapper<CodingProblemPublish> dupCheck = new LambdaQueryWrapper<>();
            dupCheck.eq(CodingProblemPublish::getProblemId, req.getProblemId())
                    .eq(CodingProblemPublish::getClassId, classId)
                    .eq(CodingProblemPublish::getIsDelete, 0);
            if (publishService.count(dupCheck) > 0) {
                skipped++;
                continue;
            }
            CodingProblemPublish publish = new CodingProblemPublish();
            publish.setProblemId(req.getProblemId());
            publish.setClassId(classId);
            publish.setChapterId(req.getChapterId());
            publish.setDeadline(req.getDeadline());
            publish.setCreatedBy(loginUser.getId());
            publishService.save(publish);
        }

        if (skipped > 0) {
            return ResultUtils.success("已跳过 " + skipped + " 个重复发布");
        }
        return ResultUtils.success("发布成功");
    }

    @Operation(summary = "学生查询已发布的编程题列表")
    @GetMapping("/student/list")
    public BaseResponse<List<CodingProblemVO>> listStudentProblems(
            @RequestParam(required = false) Long courseId,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (loginUser.getClassId() == null) {
            return ResultUtils.success(Collections.emptyList());
        }

        // 查询本班级的发布记录
        Date now = new Date();
        LambdaQueryWrapper<CodingProblemPublish> pubWrapper = new LambdaQueryWrapper<>();
        pubWrapper.eq(CodingProblemPublish::getClassId, loginUser.getClassId())
                .eq(CodingProblemPublish::getIsDelete, 0)
                .and(w -> w.isNull(CodingProblemPublish::getDeadline)
                        .or()
                        .ge(CodingProblemPublish::getDeadline, now));
        List<CodingProblemPublish> publishes = publishService.list(pubWrapper);

        if (publishes.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }

        Set<Long> problemIds = publishes.stream()
                .map(CodingProblemPublish::getProblemId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<CodingProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CodingProblem::getId, problemIds)
                .eq(CodingProblem::getIsDelete, 0);
        if (courseId != null) {
            wrapper.eq(CodingProblem::getCourseId, courseId);
        }
        wrapper.orderByDesc(CodingProblem::getCreateTime);

        List<CodingProblem> problems = problemService.list(wrapper);
        List<CodingProblemVO> voList = problems.stream().map(this::toSimpleVO).collect(Collectors.toList());

        // 构建本班级发布记录的映射 (problemId -> publish)，用于取 deadline
        Map<Long, CodingProblemPublish> pubMap = publishes.stream()
                .collect(Collectors.toMap(CodingProblemPublish::getProblemId, p -> p, (a, b) -> a));
        for (CodingProblemVO vo : voList) {
            CodingProblemPublish pub = pubMap.get(vo.getId());
            if (pub != null) {
                vo.setDeadline(pub.getDeadline());
            }
        }

        // 叠加当前学生的提交统计
        if (!voList.isEmpty()) {
            List<Long> voProblemIds = voList.stream().map(CodingProblemVO::getId).collect(Collectors.toList());
            LambdaQueryWrapper<CodingSubmission> subWrapper = new LambdaQueryWrapper<>();
            subWrapper.in(CodingSubmission::getProblemId, voProblemIds)
                    .eq(CodingSubmission::getStudentId, loginUser.getId())
                    .eq(CodingSubmission::getIsDelete, 0);
            List<CodingSubmission> mySubmissions = submissionService.list(subWrapper);
            Map<Long, List<CodingSubmission>> grouped = mySubmissions.stream()
                    .collect(Collectors.groupingBy(CodingSubmission::getProblemId));
            for (CodingProblemVO vo : voList) {
                List<CodingSubmission> subs = grouped.get(vo.getId());
                if (subs == null || subs.isEmpty()) {
                    vo.setMyAttemptCount(0);
                    vo.setMyBestScore(null);
                } else {
                    vo.setMyAttemptCount(subs.size());
                    vo.setMyBestScore(subs.stream()
                            .map(CodingSubmission::getFinalScore)
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo).orElse(null));
                }
            }
        }

        return ResultUtils.success(voList);
    }

    private CodingProblemVO toSimpleVO(CodingProblem p) {
        CodingProblemVO vo = new CodingProblemVO();
        vo.setId(p.getId());
        vo.setTitle(p.getTitle());
        vo.setDescription(p.getDescription());
        vo.setDifficulty(p.getDifficulty());
        vo.setTimeLimitMs(p.getTimeLimitMs());
        vo.setMemoryLimitKb(p.getMemoryLimitKb());
        vo.setCourseId(p.getCourseId());
        vo.setSemesterLabel(p.getSemesterLabel());
        vo.setIsPublic(p.getIsPublic());
        vo.setCreateTime(p.getCreateTime());
        try {
            vo.setLanguages(objectMapper.readValue(p.getLanguages(), new TypeReference<List<String>>() {}));
        } catch (JsonProcessingException e) {
            vo.setLanguages(Collections.emptyList());
        }
        return vo;
    }

    private CodingProblemPublish requireStudentPublish(Long problemId, User loginUser) {
        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可查看已发布编程题");
        }
        if (loginUser.getClassId() == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前学生未绑定班级，无法查看该编程题");
        }

        LambdaQueryWrapper<CodingProblemPublish> pubWrapper = new LambdaQueryWrapper<>();
        pubWrapper.eq(CodingProblemPublish::getProblemId, problemId)
                .eq(CodingProblemPublish::getClassId, loginUser.getClassId())
                .eq(CodingProblemPublish::getIsDelete, 0)
                .last("limit 1");
        CodingProblemPublish publish = publishService.getOne(pubWrapper, false);
        if (publish == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该编程题未发布给你所在班级");
        }
        return publish;
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUser;
    }

    private Course requireOwnedCourse(Long courseId, User loginUser) {
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        roleAuthorizationService.requireOwner(loginUser, course.getTeacherId(), "课程");
        return course;
    }

    private String normalizeSemesterLabel(String semesterLabel) {
        if (semesterLabel == null) {
            return null;
        }
        String trimmed = semesterLabel.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
