package com.tps.controller;

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.dto.notification.NotificationResponse;
import com.tps.entity.Notification;
import com.tps.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ApiResponse<?> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notifications = (type == null || type.isBlank())
                ? notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        return ApiResponse.success(PageResponse.from(
                notifications.map(this::toResponse)
        ));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<?> markRead(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    public ApiResponse<?> markAllRead(@AuthenticationPrincipal Long userId) {
        notificationRepository.markAllReadByUserId(userId);
        return ApiResponse.success();
    }

    @PatchMapping("/read-all")
    public ApiResponse<?> patchMarkAllRead(@AuthenticationPrincipal Long userId) {
        notificationRepository.markAllReadByUserId(userId);
        return ApiResponse.success();
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
