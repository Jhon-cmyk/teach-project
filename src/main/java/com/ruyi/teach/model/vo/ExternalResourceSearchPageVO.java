package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExternalResourceSearchPageVO implements Serializable {

    private List<ExternalResourceSearchItemVO> records = new ArrayList<>();

    private long total;

    private long current;

    private long pageSize;

    private String supportNotice;

    private static final long serialVersionUID = 1L;
}
