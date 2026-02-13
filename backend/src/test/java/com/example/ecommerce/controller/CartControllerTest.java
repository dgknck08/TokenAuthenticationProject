package com.example.ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.example.ecommerce.auth.exception.JwtValidationException;
import com.example.ecommerce.auth.service.JwtValidationService;
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
    private JwtValidationService jwtValidationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private CartController cartController;

    private CartDto sampleCart() {
        return new CartDto(List.of(), 0, BigDecimal.ZERO, "guest");
    }

    @Test
    void getCart_ShouldReturnAuthenticatedCart_WhenTokenIsValid() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtValidationService.validateToken("token")).thenReturn(true);
        when(jwtValidationService.getUserIdFromToken("token")).thenReturn(10L);
        when(cartService.getCartByUserId(10L)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.getCart(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).getCartByUserId(10L);
    }

    @Test
    void getCart_ShouldReturnGuestCart_WhenNoToken() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-1");
        when(cartService.getGuestCart("sess-1")).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.getCart(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).getGuestCart("sess-1");
    }

    @Test
    void getCart_ShouldThrow_WhenTokenInvalid() {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        when(jwtValidationService.validateToken("bad")).thenReturn(false);

        assertThrows(JwtValidationException.class, () -> cartController.getCart(request));
    }

    @Test
    void addItemToCart_ShouldUseAuthenticatedFlow() {
        AddToCartRequest add = new AddToCartRequest();
        add.setProductId(100L);
        add.setQuantity(2);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtValidationService.validateToken("token")).thenReturn(true);
        when(jwtValidationService.getUserIdFromToken("token")).thenReturn(11L);
        when(cartService.addItemToCart(11L, 100L, 2)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.addItemToCart(add, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).addItemToCart(11L, 100L, 2);
    }

    @Test
    void addItemToCart_ShouldUseGuestFlow() {
        AddToCartRequest add = new AddToCartRequest();
        add.setProductId(200L);
        add.setQuantity(1);

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-2");
        when(cartService.addItemToGuestCart("sess-2", 200L, 1)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.addItemToCart(add, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).addItemToGuestCart("sess-2", 200L, 1);
    }

    @Test
    void updateCartItem_ShouldUseGuestFlow() {
        UpdateCartItemRequest update = new UpdateCartItemRequest();
        update.setQuantity(3);

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-3");
        when(cartService.updateGuestCartItem("sess-3", 300L, 3)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.updateCartItem(300L, update, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).updateGuestCartItem("sess-3", 300L, 3);
    }

    @Test
    void removeItemFromCart_ShouldUseAuthenticatedFlow() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtValidationService.validateToken("token")).thenReturn(true);
        when(jwtValidationService.getUserIdFromToken("token")).thenReturn(20L);
        when(cartService.removeItemFromCart(20L, 400L)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.removeItemFromCart(400L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).removeItemFromCart(20L, 400L);
    }

    @Test
    void clearCart_ShouldUseGuestFlow() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("sess-4");

        ResponseEntity<Void> response = cartController.clearCart(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).clearGuestCart("sess-4");
    }

    @Test
    void mergeGuestCart_ShouldReturnUnauthorized_WhenUserIsNotAuthenticated() {
        when(request.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<CartDto> response = cartController.mergeGuestCart(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void mergeGuestCart_ShouldReturnBadRequest_WhenNoSession() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtValidationService.validateToken("token")).thenReturn(true);
        when(jwtValidationService.getUserIdFromToken("token")).thenReturn(22L);
        when(request.getSession(false)).thenReturn(null);

        ResponseEntity<CartDto> response = cartController.mergeGuestCart(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void mergeGuestCart_ShouldReturnOk_WhenAuthenticatedAndSessionExists() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtValidationService.validateToken("token")).thenReturn(true);
        when(jwtValidationService.getUserIdFromToken("token")).thenReturn(23L);
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("sess-5");
        when(cartService.mergeGuestCartToUserCart("sess-5", 23L)).thenReturn(sampleCart());

        ResponseEntity<CartDto> response = cartController.mergeGuestCart(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).mergeGuestCartToUserCart("sess-5", 23L);
    }
}
