package com.example.ecommerce.security;

import com.example.ecommerce.common.trace.CorrelationIdContext;
import com.example.ecommerce.common.trace.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrelationIdFilterTest {

    @Test
    void shouldGenerateCorrelationIdWhenHeaderMissing() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String header = response.getHeader(CorrelationIdContext.HEADER_NAME);
        assertNotNull(header);
    }

    @Test
    void shouldReuseIncomingCorrelationIdHeader() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader(CorrelationIdContext.HEADER_NAME, "cid-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("cid-123", response.getHeader(CorrelationIdContext.HEADER_NAME));
    }
}
