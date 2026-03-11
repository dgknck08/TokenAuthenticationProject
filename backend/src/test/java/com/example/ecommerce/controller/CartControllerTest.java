package com.example.ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private CartController cartController;

    @BeforeEach
    void setUp() {
        cartController = new CartController(cartService, false);
    }

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

        ResponseEntity<CartDto> responseEntity = cartController.getCart(request, response, auth);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).getCartByUserId(10L);
    }

    @Test
    void getCart_ShouldReturnGuestCart_WhenAuthenticationMissing() {
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("guest_cart_id", "guest-1") });
        when(cartService.getGuestCart("guest-1")).thenReturn(sampleCart());

        ResponseEntity<CartDto> responseEntity = cartController.getCart(request, response, null);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).getGuestCart("guest-1");
    }

    @Test
    void getCart_ShouldReturnGuestCart_WhenAuthenticationAnonymous() {
        Authentication auth = anonymousUser();
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("guest_cart_id", "guest-anon") });
        when(cartService.getGuestCart("guest-anon")).thenReturn(sampleCart());

        ResponseEntity<CartDto> responseEntity = cartController.getCart(request, response, auth);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).getGuestCart("guest-anon");
    }

    @Test
    void addItemToCart_ShouldUseAuthenticatedFlow() {
        AddToCartRequest add = new AddToCartRequest();
        add.setProductId(100L);
        add.setQuantity(2);
        Authentication auth = authenticatedUser(11L, "bob");

        when(cartService.addItemToCart(11L, 100L, 2)).thenReturn(sampleCart());

        ResponseEntity<CartDto> responseEntity = cartController.addItemToCart(add, request, response, auth);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).addItemToCart(11L, 100L, 2);
    }

    @Test
    void addItemToCart_ShouldUseGuestFlow() {
        AddToCartRequest add = new AddToCartRequest();
        add.setProductId(200L);
        add.setQuantity(1);
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("guest_cart_id", "guest-2") });
        when(cartService.addItemToGuestCart("guest-2", 200L, 1)).thenReturn(sampleCart());

        ResponseEntity<CartDto> responseEntity = cartController.addItemToCart(add, request, response, null);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).addItemToGuestCart("guest-2", 200L, 1);
    }

    @Test
    void updateCartItem_ShouldUseGuestFlow() {
        UpdateCartItemRequest update = new UpdateCartItemRequest();
        update.setQuantity(3);
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("guest_cart_id", "guest-3") });
        when(cartService.updateGuestCartItem("guest-3", 300L, 3)).thenReturn(sampleCart());

        ResponseEntity<CartDto> responseEntity = cartController.updateCartItem(300L, update, request, response, null);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).updateGuestCartItem("guest-3", 300L, 3);
    }

    @Test
    void removeItemFromCart_ShouldUseAuthenticatedFlow() {
        Authentication auth = authenticatedUser(20L, "charlie");
        when(cartService.removeItemFromCart(20L, 400L)).thenReturn(sampleCart());

        ResponseEntity<CartDto> responseEntity = cartController.removeItemFromCart(400L, request, response, auth);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).removeItemFromCart(20L, 400L);
    }

    @Test
    void clearCart_ShouldUseGuestFlow() {
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("guest_cart_id", "guest-4") });

        ResponseEntity<Void> responseEntity = cartController.clearCart(request, response, null);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).clearGuestCart("guest-4");
    }

    @Test
    void mergeGuestCart_ShouldReturnUnauthorized_WhenUserIsNotAuthenticated() {
        ResponseEntity<CartDto> responseEntity = cartController.mergeGuestCart(request, response, null);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
    }

    @Test
    void mergeGuestCart_ShouldReturnBadRequest_WhenNoSession() {
        Authentication auth = authenticatedUser(22L, "david");
        when(request.getCookies()).thenReturn(null);

        ResponseEntity<CartDto> responseEntity = cartController.mergeGuestCart(request, response, auth);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void mergeGuestCart_ShouldReturnOk_WhenAuthenticatedAndSessionExists() {
        Authentication auth = authenticatedUser(23L, "eve");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("guest_cart_id", "guest-5") });
        when(cartService.mergeGuestCartToUserCart("guest-5", 23L)).thenReturn(sampleCart());

        ResponseEntity<CartDto> responseEntity = cartController.mergeGuestCart(request, response, auth);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(cartService).mergeGuestCartToUserCart("guest-5", 23L);
    }

    @Test
    void mergeGuestCart_ShouldReturnUnauthorized_WhenPrincipalTypeIsUnexpected() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("string-principal");

        ResponseEntity<CartDto> responseEntity = cartController.mergeGuestCart(request, response, auth);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
    }
}
