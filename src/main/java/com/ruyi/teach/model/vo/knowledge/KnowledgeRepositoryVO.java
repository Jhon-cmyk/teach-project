package com.ruyi.teach.model.vo.knowledge;

public record KnowledgeRepositoryVO(
        String repoId,
        String repoName,
        String repoDesc,
        String repoTags,
        String createTime
) {
}
