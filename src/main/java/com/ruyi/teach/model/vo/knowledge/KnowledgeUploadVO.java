package com.ruyi.teach.model.vo.knowledge;

public record KnowledgeUploadVO(
        String fileId,
        String fileName,
        String parseType,
        String fileStatus
) {
}
