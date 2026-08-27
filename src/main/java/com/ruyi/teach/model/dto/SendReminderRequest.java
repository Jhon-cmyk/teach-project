package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendReminderRequest {

    @Size(max = 500, message = "提醒内容长度不能超过 500 位")
    private String message;

    @Positive(message = "班级 ID 必须为正数")
    private Long classId;
}
