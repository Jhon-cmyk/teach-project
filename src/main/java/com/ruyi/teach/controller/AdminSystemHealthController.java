package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiModelConfigMapper;
import com.ruyi.teach.model.entity.AiModelConfig;
import com.ruyi.teach.model.entity.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/system-health")
public class AdminSystemHealthController {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private AiModelConfigMapper aiModelConfigMapper;

    @Value("${aliyun.oss.endpoint:}")
    private String ossEndpoint;

    @Value("${aliyun.oss.bucket-name:}")
    private String ossBucket;

    @Value("${aliyun.asr.app-key:}")
    private String asrAppKey;

    @Value("${judge0.base-url:}")
    private String judgeBaseUrl;

    @GetMapping("/overview")
    public BaseResponse<HealthOverviewVO> overview(HttpServletRequest request) {
        getAdminLoginUser(request);

        List<HealthItemVO> items = new ArrayList<>();
        items.add(checkDatabase());
        items.add(checkModelConfigs());
        items.add(configStatus("OSS 对象存储", "storage", StringUtils.isNotBlank(ossEndpoint) && StringUtils.isNotBlank(ossBucket),
                StringUtils.isNotBlank(ossEndpoint) && StringUtils.isNotBlank(ossBucket)
                        ? ossEndpoint + " / " + ossBucket
                        : "未配置 OSS endpoint 或 bucket"));
        items.add(configStatus("ASR 语音识别", "asr", StringUtils.isNotBlank(asrAppKey),
                StringUtils.isNotBlank(asrAppKey) ? "已配置 app-key" : "未配置 app-key"));
        items.add(configStatus("Judge0 判题服务", "judge", StringUtils.isNotBlank(judgeBaseUrl),
                StringUtils.isNotBlank(judgeBaseUrl) ? judgeBaseUrl : "未配置判题服务地址"));

        long abnormalCount = items.stream().filter(item -> !"normal".equals(item.getStatus())).count();
        HealthOverviewVO vo = new HealthOverviewVO();
        vo.setItems(items);
        vo.setTotal(items.size());
        vo.setAbnormalCount(abnormalCount);
        vo.setCheckedAt(LocalDateTime.now(BEIJING_ZONE).format(DATE_TIME_FORMATTER));
        return ResultUtils.success(vo);
    }

    private HealthItemVO checkDatabase() {
        try {
            Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            String databaseTime = jdbcTemplate.queryForObject("SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')", String.class);
            return item("数据库连接", "database", value != null && value == 1 ? "normal" : "warning",
                    "MySQL " + StringUtils.defaultIfBlank(databaseName, "teach_platform") + " 可用，数据库时间 " + databaseTime);
        } catch (Exception e) {
            return item("数据库连接", "database", "error", e.getMessage());
        }
    }

    private HealthItemVO checkModelConfigs() {
        long total = aiModelConfigMapper.selectCount(new QueryWrapper<AiModelConfig>().eq("isDelete", 0));
        long enabled = aiModelConfigMapper.selectCount(new QueryWrapper<AiModelConfig>()
                .eq("enabled", 1)
                .eq("isDelete", 0));
        return item("AI 模型配置", "model", enabled > 0 ? "normal" : "warning",
                enabled > 0 ? "共 " + total + " 个配置，已启用 " + enabled + " 个" : "当前没有启用的模型配置");
    }

    private HealthItemVO configStatus(String name, String key, boolean ok, String detail) {
        return item(name, key, ok ? "normal" : "warning", detail);
    }

    private HealthItemVO item(String name, String key, String status, String detail) {
        HealthItemVO vo = new HealthItemVO();
        vo.setName(name);
        vo.setKey(key);
        vo.setStatus(status);
        vo.setDetail(detail);
        return vo;
    }

    private User getAdminLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可访问");
        }
        return loginUser;
    }

    @Data
    public static class HealthOverviewVO {
        private Integer total;
        private Long abnormalCount;
        private String checkedAt;
        private List<HealthItemVO> items;
    }

    @Data
    public static class HealthItemVO {
        private String key;
        private String name;
        private String status;
        private String detail;
    }
}
