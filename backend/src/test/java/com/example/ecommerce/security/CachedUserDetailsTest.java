package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.CachedUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachedUserDetailsTest {

    private UserDetails source(boolean enabled, boolean nonLocked) {
        return User.withUsername("alice")
                .password("secret")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .disabled(!enabled)
                .accountLocked(!nonLocked)
                .build();
    }

    @Test
    void from_shouldCopyFieldsFromUserDetails() {
        CachedUserDetails cached = CachedUserDetails.from(source(true, true));

        assertEquals("alice", cached.getUsername());
        assertEquals("secret", cached.getPassword());
        assertTrue(cached.isEnabled());
        assertTrue(cached.isAccountNonLocked());
        assertTrue(cached.isAccountNonExpired());
        assertTrue(cached.isCredentialsNonExpired());
        assertEquals(1, cached.getAuthorities().size());
        assertEquals("ROLE_USER", cached.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void from_shouldReflectDisabledAndLockedFlags() {
        CachedUserDetails cached = CachedUserDetails.from(source(false, false));
        assertFalse(cached.isEnabled());
        assertFalse(cached.isAccountNonLocked());
    }

    @Test
    void from_shouldReturnSameInstanceWhenAlreadyCached() {
        CachedUserDetails cached = CachedUserDetails.from(source(true, true));
        assertSame(cached, CachedUserDetails.from(cached));
    }
}
