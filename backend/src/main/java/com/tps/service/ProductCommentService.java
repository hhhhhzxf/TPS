package com.tps.service;

/**
 * 文件说明：商品详情留言业务服务，留言不参与订单评价和信用分计算。
 */

import com.tps.dto.product.ProductCommentRequest;
import com.tps.dto.product.ProductCommentResponse;
import com.tps.entity.Product;
import com.tps.entity.ProductComment;
import com.tps.entity.User;
import com.tps.exception.BusinessException;
import com.tps.repository.ProductCommentRepository;
import com.tps.repository.ProductRepository;
import com.tps.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCommentService {

    private final ProductCommentRepository productCommentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    public Page<ProductCommentResponse> list(Long productId, Long viewerId, int page, int size) {
        ensureProductExists(productId);
        Pageable pageable = PageRequest.of(page, size);
        return productCommentRepository
                .findByProductIdAndStatusOrderByCreatedAtDesc(
                        productId,
                        ProductComment.CommentStatus.VISIBLE,
                        pageable)
                .map(comment -> toResponse(comment, viewerId));
    }

    @Transactional
    public ProductCommentResponse create(Long productId, Long userId, ProductCommentRequest request) {
        ensureProductExists(productId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (Boolean.TRUE.equals(user.getMuted())) {
            throw new IllegalArgumentException("账号已被禁止发言，请联系管理员");
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        ProductComment comment = new ProductComment();
        comment.setProductId(productId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setImageUrls(serializeImageUrls(request.getImageUrls()));
        productCommentRepository.save(comment);
        return toResponse(comment, userId);
    }

    @Transactional
    public void delete(Long productId, Long commentId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("商品不存在"));
        ProductComment comment = productCommentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.notFound("评论不存在"));
        if (!comment.getProductId().equals(productId)) {
            throw BusinessException.notFound("评论不存在");
        }
        if (!comment.getUserId().equals(userId) && !product.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作");
        }
        if (comment.getStatus() == ProductComment.CommentStatus.DELETED) {
            return;
        }
        comment.setStatus(ProductComment.CommentStatus.DELETED);
        productCommentRepository.save(comment);
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw BusinessException.notFound("商品不存在");
        }
    }

    private ProductCommentResponse toResponse(ProductComment comment, Long viewerId) {
        ProductCommentResponse response = new ProductCommentResponse();
        response.setId(comment.getId());
        response.setProductId(comment.getProductId());
        response.setUserId(comment.getUserId());
        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            response.setUserNickname(user.getNickname());
            response.setUserAvatar(fileService.toAbsoluteUrl(user.getAvatarUrl()));
        });
        response.setContent(comment.getContent());
        response.setImageUrls(deserializeImageUrls(comment.getImageUrls()));
        response.setStatus(comment.getStatus().name());
        response.setMine(viewerId != null && viewerId.equals(comment.getUserId()));
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }

    private String serializeImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        return urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .limit(3)
                .collect(Collectors.joining(","));
    }

    private List<String> deserializeImageUrls(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .map(fileService::toAbsoluteUrl)
                .toList();
    }
}
