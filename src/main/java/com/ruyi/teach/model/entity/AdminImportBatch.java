package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_import_batch")
public class AdminImportBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("import_type")
    private String importType;

    @TableField("file_name")
    private String fileName;

    @TableField("created_count")
    private Integer createdCount;

    @TableField("skipped_count")
    private Integer skippedCount;

    @TableField("error_count")
    private Integer errorCount;

    @TableField("error_json")
    private String errorJson;

    @TableField("status")
    private String status;

    @TableField("admin_id")
    private Long adminId;

    @TableField("admin_account")
    private String adminAccount;

    @TableField("admin_name")
    private String adminName;

    @TableField("request_ip")
    private String requestIp;

    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
