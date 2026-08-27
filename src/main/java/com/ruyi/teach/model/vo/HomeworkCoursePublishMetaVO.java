package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class HomeworkCoursePublishMetaVO {
    private List<HomeworkPublishChapterOptionVO> chapterList;
    private List<HomeworkPublishClassOptionVO> classList;
    private List<HomeworkPublishQuizOptionVO> quizList;
}