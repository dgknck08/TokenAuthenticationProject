package com.example.ecommerce.auth.repository;

import com.example.ecommerce.auth.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    //pageable ile parçali sekilde çekme işlemi
	
	//userid->yaratilmasüresine göre
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // username->yaratilmasüresine göre
    Page<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    // Belirtilen action türü ve belirtilen zamandan sonraki audit kayitlari
    @Query("SELECT al FROM AuditLog al WHERE al.action = :action AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findByActionAndCreatedAtAfter(@Param("action") AuditLog.AuditAction action, @Param("since") Instant since);

    // Kullanıcı IDsine ait belirtilen tarih aralığındaki audit kayitlari
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    List<AuditLog> findUserActivityInDateRange(@Param("userId") Long userId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
