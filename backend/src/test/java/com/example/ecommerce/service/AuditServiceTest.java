package com.example.ecommerce.service;

import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.repository.AuditLogRepository;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.common.trace.CorrelationIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    @AfterEach
    void cleanup() {
        CorrelationIdContext.clear();
    }

    @Test
    void logSystemEvent_shouldIncludeCorrelationIdAndCategory() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        AuditService service = new AuditService(repository, new ObjectMapper());
        CorrelationIdContext.set("cid-456");

        service.logSystemEvent(1L, "alice", AuditLog.AuditAction.ORDER_CREATED, "Order event", Map.of("orderId", 10));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        String details = captor.getValue().getDetails();
        assertTrue(details.contains("\"correlationId\":\"cid-456\""));
        assertTrue(details.contains("\"eventCategory\":\"SYSTEM\""));
    }

    @Test
    void logAuthEvent_shouldIncludeCorrelationIdAndCategory() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        AuditService service = new AuditService(repository, new ObjectMapper());
        CorrelationIdContext.set("cid-auth");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("User-Agent", "JUnit");
        service.logAuthEvent(2L, "bob", AuditLog.AuditAction.USER_LOGIN_SUCCESS, "ok", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        String details = captor.getValue().getDetails();
        assertTrue(details.contains("\"correlationId\":\"cid-auth\""));
        assertTrue(details.contains("\"eventCategory\":\"AUTH\""));
    }
}
