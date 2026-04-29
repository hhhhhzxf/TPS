package com.tps.websocket;

import com.tps.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("WebSocket 未认证");
        }
        Long senderId = Long.valueOf(principal.getName());
        var savedMessage = messageService.sendMessage(
                senderId,
                chatMessage.getConversationId(),
                chatMessage.getContent(),
                chatMessage.getType() != null ? chatMessage.getType() : "TEXT"
        );
        chatMessage.setId(savedMessage.getId());
        chatMessage.setSenderId(senderId);
        chatMessage.setConversationId(savedMessage.getConversationId());
        chatMessage.setContent(savedMessage.getContent());
        chatMessage.setType(savedMessage.getType().name());
        chatMessage.setCreatedAt(savedMessage.getCreatedAt().toString());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + savedMessage.getConversationId(),
                chatMessage
        );
    }
}
