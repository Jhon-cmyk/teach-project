package com.ruyi.teach.model.vo.knowledge;

public record KnowledgeFileVO(
        String fileId,
        String fileName,
        String fileType,
        String fileStatus,
        String extName,
        Integer quantity,
        String expirationStatus,
        String createTime,
        String expireTime
) {
}
