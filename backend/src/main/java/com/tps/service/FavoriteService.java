package com.tps.service;

import com.tps.dto.product.ProductResponse;
import com.tps.entity.Favorite;
import com.tps.entity.Product;
import com.tps.repository.FavoriteRepository;
import com.tps.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional
    public boolean toggle(Long userId, Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            favoriteRepository.deleteByUserIdAndProductId(userId, productId);
            productRepository.decrementFavoriteCount(productId);
            return false;
        } else {
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            favoriteRepository.save(fav);
            productRepository.incrementFavoriteCount(productId);
            return true;
        }
    }

    @Transactional
    public void add(Long userId, Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            favoriteRepository.save(fav);
            productRepository.incrementFavoriteCount(productId);
        }
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            favoriteRepository.deleteByUserIdAndProductId(userId, productId);
            productRepository.decrementFavoriteCount(productId);
        }
    }

    public boolean isFavorited(Long userId, Long productId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    public List<ProductResponse> myFavorites(Long userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(f -> productRepository.findById(f.getProductId()).orElse(null))
                .filter(p -> p != null)
                .map(p -> productService.getDetailWithoutIncrement(p.getId(), userId))
                .collect(Collectors.toList());
    }
}
