package com.example.ecommerce.auth.dto;

import java.util.Set;

import com.example.ecommerce.auth.enums.Role;

public class UserDto {
    private String username;
    private String email;
    private Set<Role> roles;
}
