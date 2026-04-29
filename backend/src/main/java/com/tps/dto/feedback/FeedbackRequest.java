package com.tps.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackRequest {
    @Size(max = 30)
    private String type;

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 1000, message = "反馈内容最多1000字")
    private String content;

    @Size(max = 100)
    private String contact;
}
