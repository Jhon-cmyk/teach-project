package com.ruyi.teach.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformTeachingCaseServiceTest {

    private final PlatformTeachingCaseService service = new PlatformTeachingCaseService();

    @Test
    void keywordScoreMatchesFullCourseKeyword() {
        int score = service.keywordScore(
                "\u6570\u636e\u7ed3\u6784\u8bfe\u7a0b\u8bbe\u8ba1\u6848\u4f8b",
                "\u7ebf\u6027\u8868\u548c\u6811\u7ed3\u6784\u9879\u76ee\u5b9e\u8df5",
                "\u672c\u6848\u4f8b\u56f4\u7ed5\u6570\u636e\u7ed3\u6784\u6559\u5b66\u5c55\u5f00",
                "example.edu.cn",
                "\u6570\u636e\u7ed3\u6784"
        );

        assertTrue(score >= 8);
    }

    @Test
    void keywordScoreDoesNotMatchBigDataBySingleWord() {
        int score = service.keywordScore(
                "\u5927\u6570\u636e\u5e73\u53f0\u6559\u5b66\u6848\u4f8b",
                "\u56f4\u7ed5\u6570\u636e\u91c7\u96c6\u4e0e\u5206\u6790",
                "\u672c\u6848\u4f8b\u4ecb\u7ecd\u5927\u6570\u636e\u5904\u7406\u6d41\u7a0b",
                "example.edu.cn",
                "\u6570\u636e\u7ed3\u6784"
        );

        assertTrue(score < 8);
    }

    @Test
    void keywordScoreMatchesJavaCourseButPenalizesJavascript() {
        int javaScore = service.keywordScore(
                "Java程序设计课程案例",
                "面向对象程序设计综合实训",
                "围绕JDK环境和Java语言完成教学项目",
                "example.edu.cn",
                "java"
        );
        int javascriptScore = service.keywordScore(
                "JavaScript前端教学案例",
                "浏览器脚本和页面交互",
                "本案例介绍JavaScript事件和DOM操作",
                "example.edu.cn",
                "java"
        );

        assertTrue(javaScore >= 8);
        assertTrue(javascriptScore < 8);
    }

    @Test
    void keywordScoreMatchesComputerNetworkAliases() {
        int score = service.keywordScore(
                "网络协议实验指导",
                "TCP/IP与Wireshark抓包案例",
                "围绕计算机网络课程开展实验教学",
                "example.edu.cn",
                "计算机网络"
        );

        assertTrue(score >= 8);
    }

    @Test
    void keywordScoreMatchesDataStructureAliases() {
        int score = service.keywordScore(
                "链表与线性表课程设计",
                "栈和队列综合实验指导",
                "使用树结构和图结构组织算法案例",
                "example.edu.cn",
                "数据结构"
        );

        assertTrue(score >= 8);
    }

    @Test
    void documentValidationRejectsHtmlDisguisedAsPdf() {
        byte[] html = "<html><body>blocked</body></html>".getBytes();

        assertFalse(service.isValidDocumentBytes(html, "case.pdf"));
    }

    @Test
    void documentValidationAcceptsPdfHeader() {
        byte[] pdf = "%PDF-1.7\n%test".getBytes();

        assertTrue(service.isValidDocumentBytes(pdf, "case.pdf"));
    }
}
