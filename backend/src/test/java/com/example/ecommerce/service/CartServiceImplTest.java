package com.example.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.cart.dto.CartDto;
import com.example.ecommerce.cart.model.Cart;
import com.example.ecommerce.cart.repository.CartRepository;
import com.example.ecommerce.cart.service.CartServiceImpl;
import com.example.ecommerce.cart.service.GuestCartService;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private GuestCartService guestCartService;
    @Mock
    private InventoryService inventoryService;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(cartRepository, userRepository, productRepository, guestCartService, inventoryService);
    }

    @Test
    void addItemToCart_createsCartAndAddsNewItem() {
        User user = User.builder().id(1L).username("alice").build();
        Product product = new Product(100L, "Phone", "desc", new BigDecimal("99.90"), "img", "Cat", 10);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        CartDto dto = cartService.addItemToCart(1L, 100L, 2);

        assertEquals(2, dto.getTotalItems());
        assertEquals(new BigDecimal("199.80"), dto.getTotalAmount());
        verify(inventoryService).ensureAvailableStock(100L, 2);
    }

    @Test
    void addItemToCart_throwsWhenProductMissing() {
        Cart cart = new Cart();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cartService.addItemToCart(1L, 999L, 1));
    }

    @Test
    void getGuestCart_delegatesToGuestCartService() {
        CartDto guest = new CartDto();
        guest.setCartType("guest");
        when(guestCartService.getGuestCart("sess-1")).thenReturn(guest);

        CartDto result = cartService.getGuestCart("sess-1");
        assertEquals("guest", result.getCartType());
        verify(guestCartService).getGuestCart("sess-1");
    }
}
