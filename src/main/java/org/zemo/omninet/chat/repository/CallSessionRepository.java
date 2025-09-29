package org.zemo.omninet.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zemo.omninet.chat.model.CallSession;
import org.zemo.omninet.chat.dto.CallStatusDTO;
import org.zemo.omninet.security.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallSessionRepository extends JpaRepository<CallSession, String> {

    @Query("SELECT c FROM CallSession c WHERE (c.caller = :user OR c.callee = :user) AND c.state IN ('INITIATING', 'RINGING', 'CONNECTING', 'CONNECTED')")
    List<CallSession> findActiveCallsForUser(@Param("user") User user);

    @Query("SELECT c FROM CallSession c WHERE c.caller = :caller AND c.callee = :callee AND c.state IN ('INITIATING', 'RINGING', 'CONNECTING', 'CONNECTED')")
    Optional<CallSession> findActiveCallBetween(@Param("caller") User caller, @Param("callee") User callee);

    @Query("SELECT c FROM CallSession c WHERE (c.caller = :user OR c.callee = :user) AND c.startTime >= :since ORDER BY c.startTime DESC")
    List<CallSession> findRecentCallsForUser(@Param("user") User user, @Param("since") LocalDateTime since);

    List<CallSession> findByStateAndStartTimeBefore(CallStatusDTO.CallState state, LocalDateTime cutoff);
}
