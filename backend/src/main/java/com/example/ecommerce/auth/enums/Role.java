package com.example.ecommerce.auth.enums;

public enum Role {
    ROLE_USER("User"),
    ROLE_ADMIN("Administrator"),
    ROLE_MODERATOR("Moderator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAdmin() {
        return this == ROLE_ADMIN;
    }

    public boolean canModerate() {
        return this == ROLE_ADMIN || this == ROLE_MODERATOR;
    }
}