package com.tps.service;

/**
 * 文件说明：业务服务层，负责封装核心业务规则、事务与对象组装。
 */

import com.tps.dto.product.ProductRequest;
import com.tps.dto.product.ProductResponse;
import com.tps.entity.Notification;
import com.tps.entity.Product;
import com.tps.entity.ProductImage;
import com.tps.entity.Report;
import com.tps.entity.User;
import com.tps.exception.BusinessException;
import com.tps.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final FileService fileService;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;

    private static final List<Set<String>> SENSITIVE_KEYWORD_GROUPS = List.of(
            Set.of("烟", "香烟", "电子烟", "烟草", "vape"),
            Set.of("酒", "白酒", "啤酒", "洋酒", "酒精"),
            Set.of("代考", "替考", "考试答案", "答案"),
            Set.of("代课", "替课", "签到", "代签"),
            Set.of("代跑", "跑腿", "代取", "代拿"),
            Set.of("管制刀具", "刀具", "匕首", "甩棍"),
            Set.of("校园贷", "贷款", "借贷", "套现"),
            Set.of("药", "处方药", "违禁药", "迷药"),
            Set.of("博彩", "赌博", "下注", "彩票")
    );

    private static final Set<String> SEARCH_BLACKLIST = Set.of(
            "烟", "香烟", "电子烟", "烟草", "vape",
            "酒", "白酒", "啤酒", "洋酒", "酒精",
            "代考", "替考", "考试答案", "答案",
            "代课", "替课", "签到", "代签",
            "管制刀具", "刀具", "匕首", "甩棍",
            "校园贷", "贷款", "借贷", "套现",
            "药", "处方药", "违禁药", "迷药",
            "博彩", "赌博", "下注", "彩票"
    );

    @Transactional
    public ProductResponse create(Long userId, ProductRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (Boolean.TRUE.equals(user.getPublishBanned())) {
            throw new IllegalArgumentException("账号已被禁止发布商品，请联系管理员");
        }
        Product product = new Product();
        product.setUserId(userId);
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setCategory(req.getCategory());
        product.setCondition(req.getCondition());
        product.setLocation(req.getLocation());
        productRepository.save(product);
        if (req.getImageUrls() != null) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProductId(product.getId());
                img.setImageUrl(req.getImageUrls().get(i));
                img.setSortOrder(i);
                productImageRepository.save(img);
            }
        }
        return toResponse(product, userId);
    }

    public Page<ProductResponse> list(int page, int size, Long currentUserId) {
        // 商品列表优先看最近擦亮，其次看发布时间，贴合二手交易首页的曝光逻辑。
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("bumpedAt"), Sort.Order.desc("createdAt")));
        return productRepository.findByStatus(Product.ProductStatus.ON_SALE, pageable)
                .map(p -> toResponse(p, currentUserId));
    }

    public Page<ProductResponse> search(String keyword, String category, BigDecimal minPrice,
                                        BigDecimal maxPrice, String condition, int page, int size, Long currentUserId) {
        if (containsBlacklistedSearchTerm(keyword)) {
            throw new IllegalArgumentException("无法搜索请重试");
        }
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), Product.ProductStatus.ON_SALE));
            if (keyword != null && !keyword.isBlank()) {
                List<Predicate> keywordPredicates = new ArrayList<>();
                for (String term : expandSearchTerms(keyword)) {
                    String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
                    keywordPredicates.add(cb.like(cb.lower(root.get("title")), pattern));
                    keywordPredicates.add(cb.like(cb.lower(root.get("description")), pattern));
                    keywordPredicates.add(cb.like(cb.lower(root.get("location")), pattern));
                    keywordPredicates.add(cb.like(cb.lower(root.get("category")), pattern));
                }
                if (!keywordPredicates.isEmpty()) {
                    predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
                }
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (condition != null && !condition.isBlank()) {
                predicates.add(cb.equal(root.get("condition"), Product.Condition.valueOf(condition)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("bumpedAt"), Sort.Order.desc("createdAt")));
        return productRepository.findAll(spec, pageable).map(p -> toResponse(p, currentUserId));
    }

    @Transactional
    public ProductResponse getDetail(Long productId, Long currentUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        productRepository.incrementViewCount(productId);
        return toResponse(product, currentUserId);
    }

    public ProductResponse getDetailWithoutIncrement(Long productId, Long currentUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        return toResponse(product, currentUserId);
    }

    @Transactional
    public ProductResponse update(Long userId, Long productId, ProductRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!product.getUserId().equals(userId)) throw new IllegalArgumentException("无权操作");
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setCategory(req.getCategory());
        product.setCondition(req.getCondition());
        product.setLocation(req.getLocation());
        productRepository.save(product);
        if (req.getImageUrls() != null) {
            productImageRepository.deleteByProductId(productId);
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProductId(productId);
                img.setImageUrl(req.getImageUrls().get(i));
                img.setSortOrder(i);
                productImageRepository.save(img);
            }
        }
        return toResponse(product, userId);
    }

    @Transactional
    public void updateStatus(Long userId, Long productId, Product.ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!product.getUserId().equals(userId)) throw new IllegalArgumentException("无权操作");
        if (status == Product.ProductStatus.SOLD) {
            throw BusinessException.conflict("商品成交状态只能由订单流程更新");
        }
        if (product.getStatus() == Product.ProductStatus.SOLD && status != Product.ProductStatus.SOLD) {
            throw BusinessException.conflict("已售出商品不能重新上架");
        }
        if (product.getStatus() == status) return;
        product.setStatus(status);
        productRepository.save(product);
    }

    @Transactional
    public ProductResponse bump(Long userId, Long productId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!product.getUserId().equals(userId)) throw new IllegalArgumentException("无权操作");
        if (product.getStatus() != Product.ProductStatus.ON_SALE) {
            throw new IllegalArgumentException("只有在售商品可以擦亮");
        }

        // 擦亮次数按天重置，并且锁定当前商品记录，避免并发点击把次数冲破每日上限。
        LocalDate today = LocalDate.now();
        if (!today.equals(product.getLastBumpDate())) {
            product.setLastBumpDate(today);
            product.setBumpCountToday(0);
        }
        int count = product.getBumpCountToday() == null ? 0 : product.getBumpCountToday();
        if (count >= 3) {
            throw new IllegalArgumentException("每件商品每天最多擦亮3次");
        }
        product.setBumpCountToday(count + 1);
        product.setBumpedAt(LocalDateTime.now());
        productRepository.save(product);
        return toResponse(product, userId);
    }

    public List<ProductResponse> myProducts(Long userId) {
        return productRepository.findByUserId(userId).stream()
                .map(p -> toResponse(p, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void report(Long userId, Long productId, String reason, List<String> evidenceImageUrls) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.getUserId().equals(userId)) {
            throw new IllegalArgumentException("不能举报自己的商品");
        }
        if (reportRepository.existsByReporterIdAndProductIdAndStatus(userId, productId, Report.ReportStatus.PENDING)) {
            return;
        }
        Report report = new Report();
        report.setReporterId(userId);
        report.setProductId(productId);
        report.setReason(reason);
        report.setEvidenceImageUrls(serializeEvidenceImageUrls(evidenceImageUrls));
        reportRepository.save(report);

        Notification notification = new Notification();
        notification.setUserId(product.getUserId());
        notification.setType("REPORT");
        notification.setTitle("商品被举报");
        notification.setContent("你的商品被用户举报，平台将进行审核");
        notificationRepository.save(notification);
    }

    private List<String> expandSearchTerms(String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(keyword.trim());
        terms.add(normalized);
        Arrays.stream(normalized.split("\\s+"))
                .filter(token -> !token.isBlank())
                .forEach(terms::add);
        for (Set<String> group : SENSITIVE_KEYWORD_GROUPS) {
            boolean hit = group.stream().map(this::normalizeKeyword).anyMatch(terms::contains);
            if (hit) {
                terms.addAll(group);
            }
        }
        return terms.stream().filter(term -> !term.isBlank()).toList();
    }

    private String normalizeKeyword(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}\\s　]+", "")
                .trim();
    }

    private boolean containsBlacklistedSearchTerm(String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized.isBlank()) return false;
        return SEARCH_BLACKLIST.stream()
                .map(this::normalizeKeyword)
                .anyMatch(normalized::contains);
    }

    private String serializeEvidenceImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        return urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .limit(3)
                .collect(Collectors.joining(","));
    }

    public ProductResponse toResponse(Product p, Long currentUserId) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setUserId(p.getUserId());
        userRepository.findById(p.getUserId()).ifPresent(u -> {
            r.setSellerNickname(u.getNickname());
            r.setSellerAvatar(fileService.toAbsoluteUrl(u.getAvatarUrl()));
            r.setSellerCreditScore(u.getCreditScore());
            r.setSellerReviewCount(reviewRepository.countByRevieweeId(u.getId()));
        });
        r.setTitle(p.getTitle());
        r.setDescription(p.getDescription());
        r.setPrice(p.getPrice());
        r.setCategory(p.getCategory());
        r.setCondition(p.getCondition() != null ? p.getCondition().name() : null);
        r.setStatus(p.getStatus().name());
        r.setLocation(p.getLocation());
        r.setViewCount(p.getViewCount());
        r.setFavoriteCount(p.getFavoriteCount());
        r.setBumpedAt(p.getBumpedAt());
        r.setTakedownReason(p.getTakedownReason());
        r.setTakedownBy(p.getTakedownBy());
        r.setTakedownAt(p.getTakedownAt());
        r.setCreatedAt(p.getCreatedAt());
        List<String> urls = productImageRepository.findByProductIdOrderBySortOrder(p.getId())
                .stream().map(ProductImage::getImageUrl).map(fileService::toAbsoluteUrl).collect(Collectors.toList());
        r.setImageUrls(urls);
        if (currentUserId != null) {
            r.setFavorited(favoriteRepository.existsByUserIdAndProductId(currentUserId, p.getId()));
        }
        return r;
    }
}
