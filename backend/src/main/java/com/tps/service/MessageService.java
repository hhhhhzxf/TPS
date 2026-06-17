package com.tps.service;

/**
 * 文件说明：业务服务层，负责封装核心业务规则、事务与对象组装。
 */

import com.tps.dto.message.ConversationResponse;
import com.tps.dto.message.MessageResponse;
import com.tps.entity.Conversation;
import com.tps.entity.Message;
import com.tps.entity.Notification;
import com.tps.entity.User;
import com.tps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    @Transactional
    public ConversationResponse getOrCreateConversation(Long userId, Long targetUserId, Long productId) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能和自己创建会话");
        }
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不存在"));

        // 会话按“商品 + 买家 + 卖家”唯一化，谁先发起聊天都落到同一条会话记录上。
        Long sellerId = product.getUserId();
        Long buyerId = userId.equals(sellerId) ? targetUserId : userId;
        if (!targetUserId.equals(sellerId) && !userId.equals(sellerId)) {
            throw new IllegalArgumentException("会话必须包含商品卖家");
        }
        Conversation conversation = conversationRepository
                .findByProductIdAndBuyerIdAndSellerId(productId, buyerId, sellerId)
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setBuyerId(buyerId);
                    c.setSellerId(sellerId);
                    c.setProductId(productId);
                    return conversationRepository.save(c);
                });
        return toConversationResponse(conversation, userId);
    }

    public Page<ConversationResponse> getConversations(Long userId, Pageable pageable) {
        return conversationRepository.findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(userId, userId, pageable)
                .map(conversation -> toConversationResponse(conversation, userId));
    }

    public List<MessageResponse> getMessages(Long userId, Long conversationId) {
        Conversation conversation = getOwnedConversation(conversationId, userId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public Message sendMessage(Long senderId, Long conversationId, String content, String type) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (Boolean.TRUE.equals(sender.getMuted())) {
            throw new IllegalArgumentException("账号已被禁止发言，请联系管理员");
        }

        // 无论消息是从 REST 兜底发来还是从 WebSocket 控制器转过来，最终都会复用同一套持久化逻辑。
        Conversation conv = getOwnedConversation(conversationId, senderId);
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setType(Message.MessageType.valueOf(type.toUpperCase()));
        messageRepository.save(msg);

        conv.setLastMessage(content);
        conv.setUpdatedAt(java.time.LocalDateTime.now());
        if (conv.getBuyerId().equals(senderId)) {
            conv.setUnreadSeller((conv.getUnreadSeller() == null ? 0 : conv.getUnreadSeller()) + 1);
        } else {
            conv.setUnreadBuyer((conv.getUnreadBuyer() == null ? 0 : conv.getUnreadBuyer()) + 1);
        }
        conversationRepository.save(conv);

        Long receiverId = conv.getBuyerId().equals(senderId) ? conv.getSellerId() : conv.getBuyerId();
        Notification n = new Notification();
        n.setUserId(receiverId);
        n.setType("MESSAGE");
        n.setTitle("新消息");
        n.setContent("你有新消息");
        notificationRepository.save(n);
        return msg;
    }

    @Transactional
    public void markRead(Long conversationId, Long userId) {
        Conversation conversation = getOwnedConversation(conversationId, userId);
        messageRepository.markReadByConversationAndReceiver(conversationId, userId);

        // 会话表单独维护买家和卖家的未读数，标记已读时要同步把当前用户那一侧计数清零。
        if (conversation.getBuyerId().equals(userId)) {
            conversation.setUnreadBuyer(0);
        } else {
            conversation.setUnreadSeller(0);
        }
        conversationRepository.save(conversation);
    }

    private Conversation getOwnedConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
            throw new IllegalArgumentException("无权限访问此会话");
        }
        return conversation;
    }

    private ConversationResponse toConversationResponse(Conversation conversation, Long currentUserId) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setConversationId(conversation.getId());
        response.setProductId(conversation.getProductId());
        productRepository.findById(conversation.getProductId()).ifPresent(product -> {
            response.setProductTitle(product.getTitle());
            response.setProductPrice(product.getPrice());
            productImageRepository.findByProductIdOrderBySortOrder(product.getId()).stream()
                    .findFirst()
                    .ifPresent(image -> {
                        String imageUrl = fileService.toAbsoluteUrl(image.getImageUrl());
                        response.setProductCover(imageUrl);
                        response.setProductImageUrl(imageUrl);
                    });
        });
        Long targetUserId = conversation.getBuyerId().equals(currentUserId)
                ? conversation.getSellerId()
                : conversation.getBuyerId();
        response.setTargetUserId(targetUserId);
        userRepository.findById(targetUserId).ifPresent(user -> {
            response.setTargetNickname(user.getNickname());
            String avatarUrl = fileService.toAbsoluteUrl(user.getAvatarUrl());
            response.setTargetAvatar(avatarUrl);
            response.setTargetAvatarUrl(avatarUrl);
        });
        response.setLastMessage(conversation.getLastMessage());
        response.setUnreadBuyer(conversation.getUnreadBuyer());
        response.setUnreadSeller(conversation.getUnreadSeller());
        response.setUnreadCount(conversation.getBuyerId().equals(currentUserId)
                ? conversation.getUnreadBuyer()
                : conversation.getUnreadSeller());
        response.setUpdatedAt(conversation.getUpdatedAt());
        return response;
    }

    public MessageResponse toResponse(Message message) {
        return toMessageResponse(message);
    }

    private MessageResponse toMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversationId());
        response.setSenderId(message.getSenderId());
        response.setContent(message.getContent());
        response.setType(message.getType().name());
        response.setIsRead(message.getIsRead());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
