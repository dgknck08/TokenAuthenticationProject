package com.example.ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.CustomUserDetails;
import com.example.ecommerce.cart.controller.CartController;
import com.example.ecommerce.cart.dto.AddToCartRequest;
import com.example.ecommerce.cart.dto.CartDto;
import com.example.ecommerce.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.cart.service.CartService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private CartController cartController;

    private CartDto sampleCart() {
        return new CartDto(List.of(), 0, BigDecimal.ZERO, "guest");
    }

    private Authentication authenticatedUser(Long userId, String username) {
        User user = User.builder()
                .id(userId)
                .username(username)
                .password("pw")
                .email(username + "@test.com")
                .firstName("Test")
                .lastName("User")
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private Authentication anonymousUser() {
        return new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    @Test
    void getCart_ShouldReturnAuthenticatedCart_WhenAuthenticationPresent() {
        Authentication auth = authenticatedUser(10L, "alice");
        when(cartService.getCartByUserId(10L)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.getCart(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).getCartByUserId(10L);
    }

    @Test
    void getCart_ShouldReturnGuestCart_WhenAuthenticationMissing() {
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-1");
        when(cartService.getGuestCart("sess-1")).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.getCart(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).getGuestCart("sess-1");
    }

    @Test
    void getCart_ShouldReturnGuestCart_WhenAuthenticationAnonymous() {
        Authentication auth = anonymousUser();
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-anon");
        when(cartService.getGuestCart("sess-anon")).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.getCart(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).getGuestCart("sess-anon");
    }

    @Test
    void addItemToCart_ShouldUseAuthenticatedFlow() {
        AddToCartRequest add = new AddToCartRequest();
        add.setProductId(100L);
        add.setQuantity(2);
        Authentication auth = authenticatedUser(11L, "bob");

        when(cartService.addItemToCart(11L, 100L, 2)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.addItemToCart(add, request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).addItemToCart(11L, 100L, 2);
    }

    @Test
    void addItemToCart_ShouldUseGuestFlow() {
        AddToCartRequest add = new AddToCartRequest();
        add.setProductId(200L);
        add.setQuantity(1);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-2");
        when(cartService.addItemToGuestCart("sess-2", 200L, 1)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.addItemToCart(add, request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).addItemToGuestCart("sess-2", 200L, 1);
    }

    @Test
    void updateCartItem_ShouldUseGuestFlow() {
        UpdateCartItemRequest update = new UpdateCartItemRequest();
        update.setQuantity(3);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-3");
        when(cartService.updateGuestCartItem("sess-3", 300L, 3)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.updateCartItem(300L, update, request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).updateGuestCartItem("sess-3", 300L, 3);
    }

    @Test
    void removeItemFromCart_ShouldUseAuthenticatedFlow() {
        Authentication auth = authenticatedUser(20L, "charlie");
        when(cartService.removeItemFromCart(20L, 400L)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.removeItemFromCart(400L, request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).removeItemFromCart(20L, 400L);
    }

    @Test
    void clearCart_ShouldUseGuestFlow() {
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-4");

        ResponseEntity<Void> response = cartController.clearCart(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).clearGuestCart("sess-4");
    }

    @Test
    void mergeGuestCart_ShouldReturnUnauthorized_WhenUserIsNotAuthenticated() {
        ResponseEntity<CartDto> response = cartController.mergeGuestCart(request, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void mergeGuestCart_ShouldReturnBadRequest_WhenNoSession() {
        Authentication auth = authenticatedUser(22L, "david");
        when(request.getSession(false)).thenReturn(null);

        ResponseEntity<CartDto> response = cartController.mergeGuestCart(request, auth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void mergeGuestCart_ShouldReturnOk_WhenAuthenticatedAndSessionExists() {
        Authentication auth = authenticatedUser(23L, "eve");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("sess-5");
        when(cartService.mergeGuestCartToUserCart("sess-5", 23L)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.mergeGuestCart(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).mergeGuestCartToUserCart("sess-5", 23L);
    }

    @Test
    void mergeGuestCart_ShouldReturnUnauthorized_WhenPrincipalTypeIsUnexpected() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("string-principal");

        ResponseEntity<CartDto> response = cartController.mergeGuestCart(request, auth);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
