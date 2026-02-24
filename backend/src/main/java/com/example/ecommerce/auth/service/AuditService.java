package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.repository.AuditLogRepository;
import com.example.ecommerce.common.trace.CorrelationIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    @Autowired
    @Lazy
    private AuditService selfProxy;
    
    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.selfProxy = this;
    }

    //(login logout vs) loglamak için kullanılan method.
    public void logAuthEvent(Long userId, String username, AuditLog.AuditAction action, 
    		String description, HttpServletRequest request) {
        //async olarak kullanım
        selfProxy.logAuthEventAsync(userId, username, action, description, request);
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logAuthEventAsync(Long userId, String username, AuditLog.AuditAction action, 
    		String description, HttpServletRequest request) {
        try {
            //Builder kullanarak audit objesi oluşturma
            AuditLog.AuditLogBuilder auditBuilder = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .description(description);
            
            if (request != null) {
                //client ip
                auditBuilder
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"));

                //ek bilgiler
                Map<String, Object> details = new HashMap<>();
                details.put("requestUri", request.getRequestURI());
                details.put("method", request.getMethod());
                details.put("sessionId", request.getSession(false) != null ? request.getSession().getId() : null);
                enrichAuditDetails(details, "AUTH");
                auditBuilder.details(objectMapper.writeValueAsString(details));
            }
            
            //audit Loglari veritabanına save ediliyor.
            AuditLog auditLog = auditBuilder.build();
            auditLogRepository.save(auditLog);
            
            logger.info("Audit event logged: {} for user: {}", action, username);
            return CompletableFuture.completedFuture(null);
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing audit details", e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            logger.error("Error logging audit event", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void logSystemEvent(Long userId, String username, AuditLog.AuditAction action, String description, Map<String, Object> details) {
        selfProxy.logSystemEventAsync(userId, username, action, description, details);
    }

    @Async("auditExecutor")
    public CompletableFuture<Void> logSystemEventAsync(Long userId, String username, AuditLog.AuditAction action,
                                                       String description, Map<String, Object> details) {
        try {
            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .description(description);

            Map<String, Object> normalizedDetails = new HashMap<>();
            if (details != null) {
                normalizedDetails.putAll(details);
            }
            enrichAuditDetails(normalizedDetails, "SYSTEM");
            if (!normalizedDetails.isEmpty()) {
                builder.details(objectMapper.writeValueAsString(normalizedDetails));
            }

            auditLogRepository.save(builder.build());
            logger.info("System audit event logged: {} by user: {}", action, username);
            return CompletableFuture.completedFuture(null);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing system audit details", e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            logger.error("Error logging system audit event", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    
    //audit loglarını getirir (userId)
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    //audit loglarını getirir (username)
    public Page<AuditLog> getUserAuditLogs(String username, Pageable pageable) {
        return auditLogRepository.findByUsernameOrderByCreatedAtDesc(username, pageable);
    }

    //IP adresini almak için kullanilan metod
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        //X-Forwarded-For: <client-ip>, <proxy1-ip>, <proxy2-ip>, ...
        // 1 ilk IP client ipsini verir.
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim(); 
        }
        // 2 eğer x-forwarded-for headeri yoksa burasi kullanilacak.
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // 3 hiç Header yoksa burasi.
        return request.getRemoteAddr();
    }

    private void enrichAuditDetails(Map<String, Object> details, String category) {
        String correlationId = CorrelationIdContext.get();
        details.putIfAbsent("eventCategory", category);
        if (correlationId != null && !correlationId.isBlank()) {
            details.putIfAbsent("correlationId", correlationId);
        }
    }
}
