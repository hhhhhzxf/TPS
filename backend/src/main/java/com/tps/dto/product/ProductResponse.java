package com.tps.dto.product;

import com.tps.entity.Product;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {
    private Long id;
    private Long userId;
    private String sellerNickname;
    private String sellerAvatar;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String condition;
    private String status;
    private String location;
    private Integer viewCount;
    private Integer favoriteCount;
    private LocalDateTime bumpedAt;
    private List<String> imageUrls;
    private boolean favorited;
    private LocalDateTime createdAt;
}
