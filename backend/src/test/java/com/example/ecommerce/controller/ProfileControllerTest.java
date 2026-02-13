package com.example.ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.example.ecommerce.profile.controller.ProfileController;
import com.example.ecommerce.profile.dto.UserProfileDto;
import com.example.ecommerce.profile.service.ProfileService;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProfileController profileController;

    @Test
    void getProfile_ShouldUseAuthenticatedUsername() {
        UserProfileDto dto = new UserProfileDto();
        dto.setUsername("dogukan");

        when(authentication.getName()).thenReturn("dogukan");
        when(profileService.getUserProfile("dogukan")).thenReturn(dto);

        UserProfileDto response = profileController.getProfile(authentication);

        assertEquals("dogukan", response.getUsername());
    }

    @Test
    void updateProfile_ShouldUseAuthenticatedUsername() {
        UserProfileDto request = new UserProfileDto();
        request.setFirstName("Dogu");
        UserProfileDto updated = new UserProfileDto();
        updated.setFirstName("Dogu");

        when(authentication.getName()).thenReturn("dogukan");
        when(profileService.updateUserProfile("dogukan", request)).thenReturn(updated);

        UserProfileDto response = profileController.updateProfile(authentication, request);

        assertEquals("Dogu", response.getFirstName());
    }

    @Test
    void deleteProfile_ShouldCallServiceWithAuthenticatedUsername() {
        when(authentication.getName()).thenReturn("dogukan");

        profileController.deleteProfile(authentication);

        verify(profileService).deleteUser("dogukan");
    }
}
