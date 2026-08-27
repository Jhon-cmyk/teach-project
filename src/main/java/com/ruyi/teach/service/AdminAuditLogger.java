package com.ruyi.teach.service;

import com.ruyi.teach.mapper.AdminAuditLogMapper;
import com.ruyi.teach.model.entity.AdminAuditLog;
import com.ruyi.teach.model.entity.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class AdminAuditLogger {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<String> IP_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    @Resource
    private AdminAuditLogMapper adminAuditLogMapper;

    public void log(User admin, String module, String action, String targetType,
                    Object targetId, String summary, HttpServletRequest request) {
        if (admin == null || admin.getId() == null) {
            return;
        }
        try {
            AdminAuditLog log = new AdminAuditLog();
            log.setAdminId(admin.getId());
            log.setAdminAccount(StringUtils.defaultString(admin.getUserAccount()));
            log.setAdminName(StringUtils.defaultIfBlank(admin.getUserName(), admin.getUserAccount()));
            log.setModule(module);
            log.setAction(action);
            log.setTargetType(targetType);
            log.setTargetId(targetId == null ? "" : String.valueOf(targetId));
            log.setSummary(StringUtils.left(StringUtils.defaultString(summary), 1000));
            log.setRequestIp(resolveIp(request));
            log.setCreateTime(LocalDateTime.now(BEIJING_ZONE));
            adminAuditLogMapper.insert(log);
        } catch (Exception ignored) {
            // Audit failures must not block the admin operation itself.
        }
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        for (String header : IP_HEADERS) {
            String ip = firstValidIp(request.getHeader(header));
            if (StringUtils.isNotBlank(ip)) {
                return normalizeLocalIp(ip);
            }
        }
        return normalizeLocalIp(StringUtils.defaultString(request.getRemoteAddr()));
    }

    private String firstValidIp(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        for (String part : value.split(",")) {
            String ip = part.trim();
            if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return "";
    }

    private String normalizeLocalIp(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
