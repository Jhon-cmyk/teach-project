package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseMindmapVO {

    private String title;

    private Node root;

    /**
     * ready / fallback
     */
    private String status;

    /**
     * yyyy-MM-dd HH:mm
     */
    private String updatedAt;

    private String sourceHash;

    @Data
    public static class Node {
        private String name;
        private List<Node> children = new ArrayList<>();
    }
}