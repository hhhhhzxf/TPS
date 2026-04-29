package com.tps.controller;

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/conversation")
    public ApiResponse<?> getOrCreateConversation(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long targetUserId,
            @RequestParam Long productId) {
        return ApiResponse.success(messageService.getOrCreateConversation(userId, targetUserId, productId));
    }

    @GetMapping("/conversations")
    public ApiResponse<?> getConversations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(messageService.getConversations(userId, PageRequest.of(page, size))));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<?> getMessages(@PathVariable Long conversationId,
                                      @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(messageService.getMessages(userId, conversationId));
    }

    @PostMapping("/{conversationId}")
    public ApiResponse<?> sendMessage(@PathVariable Long conversationId,
                                      @AuthenticationPrincipal Long userId,
                                      @RequestParam String content,
                                      @RequestParam(defaultValue = "TEXT") String type) {
        return ApiResponse.success(messageService.toResponse(
                messageService.sendMessage(userId, conversationId, content, type)
        ));
    }

    @PutMapping("/{conversationId}/read")
    public ApiResponse<?> markRead(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Long userId) {
        messageService.markRead(conversationId, userId);
        return ApiResponse.success();
    }
}
