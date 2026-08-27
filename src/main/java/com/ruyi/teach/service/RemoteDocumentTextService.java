package com.ruyi.teach.service;

import com.ruyi.teach.client.RemoteResourceClient;
import com.ruyi.teach.util.CaseDocumentTextExtractor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class RemoteDocumentTextService {

    private static final int MAX_DOCUMENT_BYTES = 50 * 1024 * 1024;

    private final RemoteResourceClient remoteResourceClient;

    public RemoteDocumentTextService(RemoteResourceClient remoteResourceClient) {
        this.remoteResourceClient = remoteResourceClient;
    }

    public String extractText(String url) {
        byte[] bytes = remoteResourceClient.downloadBytesOrEmpty(
                "document-text-extractor",
                url,
                MAX_DOCUMENT_BYTES,
                Duration.ofSeconds(20)
        );
        if (bytes.length == 0) {
            return "";
        }
        String lowerUrl = StringUtils.defaultString(url).toLowerCase(Locale.ROOT);
        if (lowerUrl.contains(".docx") || lowerUrl.contains(".doc") || lowerUrl.contains(".pdf")) {
            return CaseDocumentTextExtractor.extractText(bytes, url);
        }
        return CaseDocumentTextExtractor.extractHtmlText(bytes);
    }
}
