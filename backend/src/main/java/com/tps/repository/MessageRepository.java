package com.tps.repository;

import com.tps.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :convId AND m.senderId != :userId")
    void markReadByConversationAndReceiver(@Param("convId") Long conversationId, @Param("userId") Long userId);
}
