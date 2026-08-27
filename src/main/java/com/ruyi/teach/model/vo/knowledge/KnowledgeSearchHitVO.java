package com.ruyi.teach.model.vo.knowledge;

public record KnowledgeSearchHitVO(
        String content,
        double score,
        String fileId,
        String fileName,
        Integer index,
        String type,
        String fileType
) {
}
