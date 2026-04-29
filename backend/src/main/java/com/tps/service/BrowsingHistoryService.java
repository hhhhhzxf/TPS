package com.tps.service;

import com.tps.dto.product.ProductResponse;
import com.tps.entity.BrowsingHistory;
import com.tps.entity.Product;
import com.tps.repository.BrowsingHistoryRepository;
import com.tps.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BrowsingHistoryService {

    private final BrowsingHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional
    public void record(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.getUserId().equals(userId)) {
            return;
        }
        BrowsingHistory history = historyRepository.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> {
                    BrowsingHistory created = new BrowsingHistory();
                    created.setUserId(userId);
                    created.setProductId(productId);
                    return created;
                });
        history.setViewedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    public Page<ProductResponse> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("viewedAt").descending());
        return historyRepository.findByUserIdOrderByViewedAtDesc(userId, pageable)
                .map(history -> productService.getDetailWithoutIncrement(history.getProductId(), userId));
    }

    @Transactional
    public void clear(Long userId) {
        historyRepository.deleteByUserId(userId);
    }

    @Transactional
    public void delete(Long userId, Long productId) {
        historyRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
