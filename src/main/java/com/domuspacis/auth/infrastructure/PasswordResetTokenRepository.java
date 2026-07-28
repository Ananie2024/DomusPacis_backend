package com.domuspacis.auth.infrastructure;

import com.domuspacis.auth.domain.PasswordResetToken;
import com.domuspacis.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.token = :token AND prt.user = :user AND prt.used = false AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidTokenByUser(@Param("token") String token, @Param("user") User user, @Param("now") LocalDateTime now);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user = :user AND prt.used = false AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidTokenForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true, prt.usedAt = :usedAt WHERE prt.token = :token AND prt.used = false")
    int markTokenAsUsed(@Param("token") String token, @Param("usedAt") LocalDateTime usedAt);

    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true, prt.usedAt = :usedAt WHERE prt.user = :user AND prt.used = false")
    int invalidateAllUserTokens(@Param("user") User user, @Param("usedAt") LocalDateTime usedAt);

    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.user = :user AND prt.used = false AND prt.expiresAt > :now")
    long countValidTokensForUser(@Param("user") User user, @Param("now") LocalDateTime now);
}