package com.ruyi.teach.model.vo;

import lombok.Data;
import java.util.Date;

@Data
public class HistoryCourseVO {
    private Long id;
    private String name;
    private String description;
    private String coverImg;
    // 巧妙利用别名：将后端的最后学习时间，直接映射为前端期望的 updateTime 字段
    private Date updateTime; 
}