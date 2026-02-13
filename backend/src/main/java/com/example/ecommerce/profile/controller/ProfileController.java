package com.example.ecommerce.profile.controller;

import com.example.ecommerce.profile.dto.UserProfileDto;
import com.example.ecommerce.profile.service.ProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserProfileDto getProfile(Authentication authentication) {
        String username = authentication.getName();
        return profileService.getUserProfile(username);
    }

    @PutMapping
    public UserProfileDto updateProfile(Authentication authentication, @RequestBody UserProfileDto updatedProfile) {
        String username = authentication.getName();
        return profileService.updateUserProfile(username, updatedProfile);
    }

    @DeleteMapping
    public void deleteProfile(Authentication authentication) {
        String username = authentication.getName();
        profileService.deleteUser(username);
    }
}
