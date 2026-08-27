package com.ruyi.teach.service;

import com.ruyi.teach.client.OssClient;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class OssService {

    private final OssClient ossClient;

    public OssService(OssClient ossClient) {
        this.ossClient = ossClient;
    }

    public String uploadFile(MultipartFile file) {
        return uploadFile(file, "common");
    }

    public String uploadFile(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            return uploadStream(
                    inputStream,
                    file.getOriginalFilename(),
                    dir,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Aliyun OSS", "文件上传服务暂时不可用", e);
        }
    }

    public String uploadBytes(byte[] bytes, String originalFilename, String dir, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传内容不能为空");
        }

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return uploadStream(inputStream, originalFilename, dir, contentType, bytes.length);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Aliyun OSS", "文件上传服务暂时不可用", e);
        }
    }

    public String uploadStream(InputStream inputStream,
                               String originalFilename,
                               String dir,
                               String contentType,
                               long contentLength) {
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传内容不能为空");
        }

        try {
            String objectName = buildObjectName(originalFilename, dir);
            String url = ossClient.upload(
                    inputStream,
                    objectName,
                    contentType,
                    contentLength
            );
            log.info("OSS stream upload succeeded: {}", url);
            return url;
        } catch (Exception e) {
            throw new ExternalServiceException("Aliyun OSS", "文件上传服务暂时不可用", e);
        }
    }

    private String buildObjectName(String originalFilename, String dir) {
        String extension = getExtension(originalFilename);
        String safeDir = normalizeDir(dir);
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        return safeDir + "/" + datePath + "/" + fileName;
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    private String normalizeDir(String dir) {
        if (dir == null || dir.trim().isEmpty()) {
            return "common";
        }
        String value = dir.trim().replace("\\", "/");
        value = value.replaceAll("^/+", "").replaceAll("/+$", "");
        value = value.replace("..", "");
        return value.isEmpty() ? "common" : value;
    }

}
