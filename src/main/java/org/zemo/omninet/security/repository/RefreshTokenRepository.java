package org.zemo.omninet.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zemo.omninet.security.model.RefreshToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserIdAndRevokedFalse(String userId);

    void deleteByExpiresAtBeforeOrRevokedTrue(LocalDateTime dateTime);

    void deleteByUserId(String userId);

    int countByUserIdAndRevokedFalse(String userId);
}
