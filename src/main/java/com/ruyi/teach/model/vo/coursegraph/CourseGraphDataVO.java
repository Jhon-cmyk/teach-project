package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

import java.util.List;

@Data
public class CourseGraphDataVO {

    private List<CourseGraphNodeVO> nodes;

    private List<CourseGraphLinkVO> links;

    private List<CourseGraphCategoryVO> categories;
}
