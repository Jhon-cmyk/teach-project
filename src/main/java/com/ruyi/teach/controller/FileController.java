package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private OssService ossService;

    @Operation(summary = "通用文件上传 (直连阿里云OSS)")
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dir", required = false, defaultValue = "common") String dir
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        if (dir == null || !dir.matches("^[A-Za-z0-9/_-]{1,64}$") || dir.contains("..")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传目录格式不合法");
        }
        String fileUrl = ossService.uploadFile(file, dir);
        return ResultUtils.success(fileUrl);
    }
}
