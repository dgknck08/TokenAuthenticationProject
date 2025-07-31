package com.example.ecommerce.auth.service;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.example.ecommerce.auth.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class JwtBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String TOKEN_METADATA_KEY = "jwt:metadata:";
    private static final String USER_TOKENS_KEY = "jwt:user_tokens:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtils jwtUtils;

    public JwtBlacklistService(RedisTemplate<String, Object> redisTemplate, JwtUtils jwtUtils) {
        this.redisTemplate = redisTemplate;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Token blackliste ekler
     */
    public void blacklistToken(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            
            Instant expiration = jwtUtils.getExpirationDate(token);
            long ttl = Duration.between(Instant.now(), expiration).getSeconds();
            
            if (ttl > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.SECONDS);
                
                // Token kullanıcının token listesinden de kaldır
                String username = jwtUtils.getUsername(token);
                removeTokenFromUserList(username, token);
                
                logger.info("Token blacklisted successfully with TTL: {} seconds", ttl);
            }
        } catch (Exception e) {
            logger.error("Error blacklisting token: {}", e.getMessage());
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

    /**
     * Token blacklist içinde var mı yok mu kontrolü
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("Error checking token blacklist status: {}", e.getMessage());
            return true; // Hata durumunda token geçersiz sayilir.
        }
    }

    /**
     * Kullanıcının tüm aktif tokenlerini blackliste ekleme
     */
    public void blacklistUserTokens(String username) {
        try {
            logger.info("Blacklisting all tokens for user: {}", username);
            
            // Kullanıcının tokenlerini alir
            Set<String> userTokens = getUserTokens(username);
            
            if (!userTokens.isEmpty()) {
                // Pipeline ile tüm tokenları blackliste ekler
                List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String token : userTokens) {
                        try {
                            Instant expiration = jwtUtils.getExpirationDate(token);
                            long ttl = Duration.between(Instant.now(), expiration).getSeconds();
                            
                            if (ttl > 0) {
                                String blacklistKey = BLACKLIST_PREFIX + token;
                                connection.setEx(
                                    blacklistKey.getBytes(StandardCharsets.UTF_8), 
                                    ttl, 
                                    "blacklisted".getBytes(StandardCharsets.UTF_8)
                                );
                            }
                        } catch (Exception e) {
                            logger.error("Error processing token for blacklist: {}", e.getMessage());
                        }
                    }
                    return null;
                });
                
                // Kullanıcının token listesini temizler
                clearUserTokens(username);
                
                logger.info("Successfully blacklisted {} tokens for user: {}", userTokens.size(), username);
            } else {
                logger.info("No active tokens found for user: {}", username);
            }
            
        } catch (Exception e) {
            logger.error("Error blacklisting user tokens: {}", e.getMessage());
            throw new RuntimeException("Failed to blacklist user tokens", e);
        }
    }

    /**
     * Token metadatasını asenkron olarak saklar
     */
    @Async("authExecutor")
    public CompletableFuture<Void> storeTokenMetadata(String token, String username, String ipAddress, String userAgent) {
        try {
            String tokenId = jwtUtils.getTokenId(token);
            String metadataKey = TOKEN_METADATA_KEY + tokenId;
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", username);
            metadata.put("ipAddress", ipAddress);
            metadata.put("userAgent", userAgent);
            metadata.put("issuedAt", Instant.now().toString());
            metadata.put("deviceFingerprint", generateDeviceFingerprint(userAgent, ipAddress));
            metadata.put("token", token); // Token'ı da metadata'ya ekle
            
            Instant expiration = jwtUtils.getExpirationDate(token);
            long ttl = Duration.between(Instant.now(), expiration).getSeconds();
            
            if (ttl > 0) {
                // Pipeline ile hem metadata'yı sakla hem de kullanıcı token listesine ekle
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    // Metadatayı saklar 
                    String metadataKeyBytes = metadataKey;
                    connection.hMSet(metadataKeyBytes.getBytes(), 
                        convertToByteMap(metadata));
                    connection.expire(metadataKeyBytes.getBytes(), ttl);
                    
                    // Kullanıcının token listesine ekler
                    String userTokensKey = USER_TOKENS_KEY + username;
                    connection.sAdd(userTokensKey.getBytes(), token.getBytes());
                    connection.expire(userTokensKey.getBytes(), ttl);
                    
                    return null;
                });
                
                logger.debug("Token metadata stored for token: {}", tokenId);
            }
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error storing token metadata", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Token metadata'sını getirir
     */
    public Map<String, Object> getTokenMetadata(String token) {
        try {
            String tokenId = jwtUtils.getTokenId(token);
            String key = TOKEN_METADATA_KEY + tokenId;
            
            Map<Object, Object> rawMetadata = redisTemplate.opsForHash().entries(key);
            Map<String, Object> metadata = new HashMap<>();
            
            rawMetadata.forEach((k, v) -> metadata.put(k.toString(), v));
            
            return metadata.isEmpty() ? null : metadata;
        } catch (Exception e) {
            logger.error("Error retrieving token metadata", e);
            return null;
        }
    }

    /**
     * Kullanıcının aktif tokenlerini getirir
     */
    public Set<String> getUserTokens(String username) {
        try {
            String key = USER_TOKENS_KEY + username;
            Set<Object> rawTokens = redisTemplate.opsForSet().members(key);
            
            Set<String> tokens = new HashSet<>();
            if (rawTokens != null) {
                rawTokens.forEach(token -> tokens.add(token.toString()));
            }
            
            return tokens;
        } catch (Exception e) {
            logger.error("Error getting user tokens: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Kullanıcının token listesinden belirli bir tokenı kaldırır
     */
    private void removeTokenFromUserList(String username, String token) {
        try {
            String key = USER_TOKENS_KEY + username;
            redisTemplate.opsForSet().remove(key, token);
        } catch (Exception e) {
            logger.error("Error removing token from user list: {}", e.getMessage());
        }
    }

    /**
     * Kullanıcının tüm token listesini temizler
     */
    private void clearUserTokens(String username) {
        try {
            String key = USER_TOKENS_KEY + username;
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("Error clearing user tokens: {}", e.getMessage());
        }
    }

    /**
     * Kullanıcının aktif token sayısını getirir
     */
    public long getUserActiveTokenCount(String username) {
        try {
            String key = USER_TOKENS_KEY + username;
            Long count = redisTemplate.opsForSet().size(key);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error getting user active token count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Tüm kullanıcıların token istatistiklerini getirir (admin için)
     */
    public Map<String, Long> getAllUserTokenStats() {
        try {
            Map<String, Long> stats = new HashMap<>();
            
            // USER_TOKENS_KEY pattern'ına uyan tüm key'leri bul
            Set<String> keys = redisTemplate.keys(USER_TOKENS_KEY + "*");
            
            if (keys != null) {
                for (String key : keys) {
                    String username = key.substring(USER_TOKENS_KEY.length());
                    Long count = redisTemplate.opsForSet().size(key);
                    stats.put(username, count != null ? count : 0);
                }
            }
            
            return stats;
        } catch (Exception e) {
            logger.error("Error getting all user token stats: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Süresi dolmuş metadata'ları temizler (scheduled job için)
     */
    public void cleanupExpiredMetadata() {
        try {
            // Bu method bir scheduled job tarafından çağrılabilir
            // Redis'in TTL özelliği otomatik temizlik yapar ama
            // manuel temizlik de yapılabilir
            logger.info("Cleanup expired metadata completed");
        } catch (Exception e) {
            logger.error("Error during metadata cleanup: {}", e.getMessage());
        }
    }

    /**
     * Device fingerprint oluşturur
     */
    private String generateDeviceFingerprint(String userAgent, String ipAddress) {
        String combined = (userAgent != null ? userAgent : "") + ":" + ipAddress;
        return String.valueOf(combined.hashCode());
    }

    /**
     * Map'i byte map'e çevirir (Redis için)
     */
    private Map<byte[], byte[]> convertToByteMap(Map<String, Object> map) {
        Map<byte[], byte[]> byteMap = new HashMap<>();
        map.forEach((key, value) -> {
            byteMap.put(key.getBytes(StandardCharsets.UTF_8), 
                       value.toString().getBytes(StandardCharsets.UTF_8));
        });
        return byteMap;
    }
}