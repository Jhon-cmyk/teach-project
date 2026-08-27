package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiModelConfigMapper;
import com.ruyi.teach.model.entity.AiModelConfig;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/model-config")
@Tag(name = "管理端接口服务配置")
public class AdminModelConfigController {

    @Resource
    private AiModelConfigMapper aiModelConfigMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Operation(summary = "接口服务配置列表")
    @GetMapping("/list")
    public BaseResponse<List<AiModelConfig>> list(HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AiModelConfig::getSortOrder)
                .orderByAsc(AiModelConfig::getId);

        return ResultUtils.success(aiModelConfigMapper.selectList(wrapper));
    }

    @Operation(summary = "更新接口服务配置")
    @PostMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody ModelConfigUpdateRequest requestBody,
                                        HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "配置 ID 不能为空");
        }

        AiModelConfig oldConfig = aiModelConfigMapper.selectById(requestBody.getId());
        if (oldConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口服务配置不存在");
        }

        validate(requestBody);

        AiModelConfig update = new AiModelConfig();
        update.setId(requestBody.getId());
        update.setInterfaceName(StringUtils.trim(requestBody.getInterfaceName()));
        update.setProvider(StringUtils.trimToEmpty(requestBody.getProvider()));
        update.setEndpointUrl(StringUtils.trim(requestBody.getEndpointUrl()));
        update.setModelName(StringUtils.trim(requestBody.getModelName()));
        update.setEnabled(Boolean.TRUE.equals(requestBody.getEnabled()) ? 1 : 0);
        update.setRemark(StringUtils.trimToEmpty(requestBody.getRemark()));

        boolean result = aiModelConfigMapper.updateById(update) > 0;
        adminAuditLogger.log(adminUser, "接口服务配置", "更新服务配置", "ai_model_config", requestBody.getId(),
                update.getInterfaceName() + " / " + update.getModelName(), request);
        return ResultUtils.success(result);
    }

    private void validate(ModelConfigUpdateRequest requestBody) {
        if (StringUtils.isBlank(requestBody.getInterfaceName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口名称不能为空");
        }
        if (StringUtils.isBlank(requestBody.getEndpointUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务地址不能为空");
        }
        String endpointUrl = requestBody.getEndpointUrl().trim();
        if (!StringUtils.startsWithIgnoreCase(endpointUrl, "http://")
                && !StringUtils.startsWithIgnoreCase(endpointUrl, "https://")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务地址必须以 http:// 或 https:// 开头");
        }
        if (StringUtils.isBlank(requestBody.getModelName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型或参数不能为空");
        }
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
    public static class ModelConfigUpdateRequest {
        private Long id;
        private String interfaceName;
        private String provider;
        private String endpointUrl;
        private String modelName;
        private Boolean enabled;
        private String remark;
    }
}
