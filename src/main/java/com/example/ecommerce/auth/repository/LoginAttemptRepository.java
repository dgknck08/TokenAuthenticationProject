package com.example.ecommerce.auth.repository;

import com.example.ecommerce.auth.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    // Kullanıcı adına ve zamana göre son giriş denemeleri (en yeniden eskiye)
    @Query("SELECT la FROM LoginAttempt la WHERE la.username = :username AND la.createdAt >= :since ORDER BY la.createdAt DESC")
    List<LoginAttempt> findRecentAttemptsByUsername(@Param("username") String username, @Param("since") Instant since);

    // IP adresine ve zamana göre son giriş denemeleri (en yeniden eskiye)
    @Query("SELECT la FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.createdAt >= :since ORDER BY la.createdAt DESC")
    List<LoginAttempt> findRecentAttemptsByIpAddress(@Param("ipAddress") String ipAddress, @Param("since") Instant since);

    // Kullanıcı adına göre belirtilen zamandan sonraki başarısız giriş denemelerinin sayısı
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username AND la.successful = false AND la.createdAt >= :since")
    long countFailedAttemptsByUsername(@Param("username") String username, @Param("since") Instant since);

    // IP adresine göre belirtilen zamandan sonraki başarısız giriş denemelerinin sayısı
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.successful = false AND la.createdAt >= :since")
    long countFailedAttemptsByIpAddress(@Param("ipAddress") String ipAddress, @Param("since") Instant since);

    // Kullanıcının son 5 başarılı giriş kaydı (en yeniden eskiye)
    @Query("SELECT la FROM LoginAttempt la WHERE la.username = :username AND la.successful = true ORDER BY la.createdAt DESC")
    List<LoginAttempt> findLastSuccessfulLogins(@Param("username") String username);
}
