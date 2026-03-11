package com.example.ecommerce.auth.service;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.Cursor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.example.ecommerce.auth.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class JwtBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String TOKEN_METADATA_KEY = "jwt:metadata:";
    private static final String USER_TOKENS_KEY = "jwt:user_tokens:";
    private static final String USER_TOKEN_REF_SEPARATOR = ":";
    
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
            String tokenHash = hashToken(token);
            String key = BLACKLIST_PREFIX + tokenHash;
            
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
            String tokenHash = hashToken(token);
            Boolean hashedExists = redisTemplate.hasKey(BLACKLIST_PREFIX + tokenHash);
            if (Boolean.TRUE.equals(hashedExists)) {
                return true;
            }

            // Backward compatibility for previously stored raw-token blacklist keys.
            Boolean legacyExists = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
            return Boolean.TRUE.equals(legacyExists);
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
            logger.info("Blacklisting all tokens for user: {}", sanitizeForLog(username));
            
            // Kullanıcının tokenlerini alir
            Set<String> userTokens = getUserTokens(username);
            
            if (!userTokens.isEmpty()) {
                // Pipeline ile tüm tokenları blackliste ekler
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String tokenRef : userTokens) {
                        try {
                            String tokenHash = extractTokenHashFromReference(tokenRef);
                            long ttl = resolveTtlSecondsFromReference(tokenRef);
                            if (tokenHash != null && ttl > 0) {
                                String blacklistKey = BLACKLIST_PREFIX + tokenHash;
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
                
                logger.info("Successfully blacklisted {} tokens for user: {}", userTokens.size(), sanitizeForLog(username));
            } else {
                logger.info("No active tokens found for user: {}", sanitizeForLog(username));
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
            
            Instant expiration = jwtUtils.getExpirationDate(token);
            long ttl = Duration.between(Instant.now(), expiration).getSeconds();
            
            if (ttl > 0) {
                String tokenHash = hashToken(token);
                String tokenRef = buildTokenReference(tokenHash, expiration);
                metadata.put("tokenHash", tokenHash);
                metadata.put("expiresAt", expiration.toString());

                // Pipeline ile hem metadata'yı sakla hem de kullanıcı token listesine ekle
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    // Metadatayı saklar 
                    String metadataKeyBytes = metadataKey;
                    connection.hMSet(metadataKeyBytes.getBytes(), 
                        convertToByteMap(metadata));
                    connection.expire(metadataKeyBytes.getBytes(), ttl);
                    
                    // Kullanıcının token listesine ekler
                    String userTokensKey = USER_TOKENS_KEY + username;
                    connection.sAdd(userTokensKey.getBytes(), tokenRef.getBytes(StandardCharsets.UTF_8));
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

    public Set<String> getUserTokens(String username) {
        try {
            String key = USER_TOKENS_KEY + username;
            Set<String> tokens = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<byte[]> rawTokens = connection.sMembers(key.getBytes(StandardCharsets.UTF_8));
                Set<String> references = new HashSet<>();
                if (rawTokens != null) {
                    rawTokens.forEach(token -> references.add(new String(token, StandardCharsets.UTF_8)));
                }
                return references;
            });

            return tokens != null ? tokens : new HashSet<>();
        } catch (Exception e) {
            logger.error("Error getting user tokens: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    private void removeTokenFromUserList(String username, String token) {
        try {
            String key = USER_TOKENS_KEY + username;
            String tokenHash = hashToken(token);
            Set<String> tokenRefs = getUserTokens(username);
            if (tokenRefs.isEmpty()) {
                return;
            }

            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] userTokensKey = key.getBytes(StandardCharsets.UTF_8);
                for (String tokenRef : tokenRefs) {
                    String refHash = extractTokenHashFromReference(tokenRef);
                    if (tokenHash.equals(refHash) || token.equals(tokenRef)) {
                        connection.sRem(userTokensKey, tokenRef.getBytes(StandardCharsets.UTF_8));
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Error removing token from user list: {}", e.getMessage());
        }
    }

    private void clearUserTokens(String username) {
        try {
            String key = USER_TOKENS_KEY + username;
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("Error clearing user tokens: {}", e.getMessage());
        }
    }


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


    public Map<String, Long> getAllUserTokenStats() {
        try {
            Map<String, Long> stats = new HashMap<>();

            ScanOptions options = ScanOptions.scanOptions()
                .match(USER_TOKENS_KEY + "*")
                .count(500)
                .build();

            try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .scan(options)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next(), StandardCharsets.UTF_8);
                    if (!key.startsWith(USER_TOKENS_KEY)) {
                        continue;
                    }
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

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredMetadata() {
        try {
            int deleted = 0;
            ScanOptions options = ScanOptions.scanOptions()
                .match(TOKEN_METADATA_KEY + "*")
                .count(500)
                .build();

            try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .scan(options)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next(), StandardCharsets.UTF_8);
                    Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttlSeconds == null || ttlSeconds <= 0) {
                        redisTemplate.delete(key);
                        deleted++;
                    }
                }
            }

            logger.info("Cleanup expired metadata completed. Deleted: {}", deleted);
        } catch (Exception e) {
            logger.error("Error during metadata cleanup: {}", e.getMessage());
        }
    }


    private String generateDeviceFingerprint(String userAgent, String ipAddress) {
        String combined = (userAgent != null ? userAgent : "") + ":" + ipAddress;
        return String.valueOf(combined.hashCode());
    }


    private Map<byte[], byte[]> convertToByteMap(Map<String, Object> map) {
        Map<byte[], byte[]> byteMap = new HashMap<>();
        map.forEach((key, value) -> {
            byteMap.put(key.getBytes(StandardCharsets.UTF_8), 
                       value.toString().getBytes(StandardCharsets.UTF_8));
        });
        return byteMap;
    }

    private String buildTokenReference(String tokenHash, Instant expiration) {
        return tokenHash + USER_TOKEN_REF_SEPARATOR + expiration.getEpochSecond();
    }

    private String extractTokenHashFromReference(String tokenReference) {
        if (tokenReference == null || tokenReference.isBlank()) {
            return null;
        }

        int separatorIndex = tokenReference.indexOf(USER_TOKEN_REF_SEPARATOR);
        if (separatorIndex > 0) {
            return tokenReference.substring(0, separatorIndex);
        }

        // Legacy format where raw JWT itself was stored in Redis set.
        if (tokenReference.contains(".")) {
            return hashToken(tokenReference);
        }

        // Legacy hash-only format.
        return tokenReference;
    }

    private long resolveTtlSecondsFromReference(String tokenReference) {
        if (tokenReference == null || tokenReference.isBlank()) {
            return -1;
        }

        int separatorIndex = tokenReference.indexOf(USER_TOKEN_REF_SEPARATOR);
        if (separatorIndex > 0 && separatorIndex + 1 < tokenReference.length()) {
            try {
                long expirationEpochSecond = Long.parseLong(tokenReference.substring(separatorIndex + 1));
                return expirationEpochSecond - Instant.now().getEpochSecond();
            } catch (NumberFormatException ignored) {
                // Fallback to legacy handling below.
            }
        }

        // Legacy format where raw JWT itself was stored in Redis set.
        if (tokenReference.contains(".")) {
            try {
                Instant expiration = jwtUtils.getExpirationDate(tokenReference);
                return Duration.between(Instant.now(), expiration).getSeconds();
            } catch (Exception ignored) {
                return -1;
            }
        }

        return -1;
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\n\\r\\t]", "_");
    }
}
