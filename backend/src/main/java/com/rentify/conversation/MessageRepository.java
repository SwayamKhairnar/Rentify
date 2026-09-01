package com.rentify.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Modifying
    @Query("update Message m set m.isRead = true where m.conversation.id = :conversationId and m.sender.id <> :userId and m.isRead = false")
    int markMessagesAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Modifying
    @Query("update Message m set m.isRead = true where m.conversation.id = :conversationId and m.sender.id <> :userId and m.isRead = false")
    int markConversationMessagesRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("select count(m) from Message m where m.conversation.id in :conversationIds and m.sender.id <> :userId and m.isRead = false")
    long countUnreadMessages(@Param("conversationIds") List<Long> conversationIds, @Param("userId") Long userId);

    @Query("select count(m) from Message m where m.conversation.id = :conversationId and m.sender.id <> :userId and m.isRead = false")
    long countUnreadInConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("""
        select count(m)
        from Message m
        where m.conversation.id in (
            select c.id
            from Conversation c
            join c.participants p
            where p.id = :userId
        )
          and m.sender.id <> :userId
          and m.isRead = false
    """)
    long countUnreadForUser(@Param("userId") Long userId);
}
