package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class IdRequest {

    @NotNull(message = "ID 不能为空")
    @Positive(message = "ID 必须为正数")
    private Long id;
}
