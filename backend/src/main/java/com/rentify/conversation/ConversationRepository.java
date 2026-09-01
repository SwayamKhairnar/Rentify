package com.rentify.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByRentalId(Long rentalId);

    @Query("""
        select distinct c
        from Conversation c
        join c.participants p
        where p.id = :userId
        order by c.lastMessageAt desc
    """)
    List<Conversation> findUserConversations(@Param("userId") Long userId);

    @Query("""
        select count(c) > 0
        from Conversation c
        join c.participants p
        where c.id = :conversationId and p.id = :userId
    """)
    boolean isParticipant(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
