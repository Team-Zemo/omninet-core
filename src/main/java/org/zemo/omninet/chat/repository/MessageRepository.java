package org.zemo.omninet.chat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zemo.omninet.chat.model.Message;
import org.zemo.omninet.security.model.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findByReceiverAndStatus(User receiver, Message.Status status);

    @Query("SELECT m FROM Message m WHERE (m.sender = :a AND m.receiver = :b) OR (m.sender = :b AND m.receiver = :a) ORDER BY m.timestamp DESC")
    Page<Message> conversation(@Param("a") User a, @Param("b") User b, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender = :other AND m.receiver = :me AND m.status <> 'READ'")
    long countUnreadFrom(@Param("me") User me, @Param("other") User other);

    @Query(value = "SELECT * FROM messages m WHERE (m.sender_id = :a AND m.receiver_id = :b) OR (m.sender_id = :b AND m.receiver_id = :a) ORDER BY m.timestamp DESC LIMIT 1", nativeQuery = true)
    Message lastBetween(@Param("a") String aId, @Param("b") String bId);

    @Query("SELECT m FROM Message m WHERE m.sender = :other AND m.receiver = :me AND m.status <> 'READ'")
    List<Message> findUnreadFrom(@Param("me") User me, @Param("other") User other);
}