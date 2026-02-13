package com.example.ecommerce.auth.enums;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    ROLE_USER("User", EnumSet.of(
            Permission.PRODUCT_READ,
            Permission.PROFILE_READ,
            Permission.PROFILE_WRITE,
            Permission.CART_READ,
            Permission.CART_WRITE
    )),
    ROLE_ADMIN("Administrator", EnumSet.allOf(Permission.class)),
    ROLE_MODERATOR("Moderator", EnumSet.of(
            Permission.PRODUCT_READ,
            Permission.PRODUCT_WRITE,
            Permission.INVENTORY_READ,
            Permission.INVENTORY_WRITE,
            Permission.AUDIT_READ
    ));

    private final String displayName;
    private final Set<Permission> permissions;

    Role(String displayName, Set<Permission> permissions) {
        this.displayName = displayName;
        this.permissions = permissions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean isAdmin() {
        return this == ROLE_ADMIN;
    }

    public boolean canModerate() {
        return this == ROLE_ADMIN || this == ROLE_MODERATOR;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}
