package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ResourceSearchPageVO implements Serializable {

    private List<ResourceSearchItemVO> records = new ArrayList<>();

    private long total;

    private long current;

    private long pageSize;

    private long videoCount;

    private long planCount;

    private long quizCount;

    private long animCount;

    private long microVideoCount;

    private long caseCount;

    private String supportNotice;

    private static final long serialVersionUID = 1L;
}
