package com.example.ecommerce.auth.security;

import com.example.ecommerce.auth.enums.Permission;
import com.example.ecommerce.auth.enums.Role;
import com.example.ecommerce.auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("serial")
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final transient User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<Role> roles = user.getRoles();
        return roles.stream()
                .flatMap(role -> Stream.concat(
                        Stream.of(new SimpleGrantedAuthority(role.name())),
                        role.getPermissions().stream()
                                .map(Permission::name)
                                .map(SimpleGrantedAuthority::new)
                ))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();  
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; 
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    public User getUser() {
        return user;
    }
}
