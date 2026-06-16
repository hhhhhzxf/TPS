package com.tps.dto.product;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReportProductRequest {
    @Size(max = 255)
    private String reason;

    @Size(max = 3, message = "最多上传3张举报凭证")
    private List<String> evidenceImageUrls;
}
