package com.example.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.ecommerce.auth.enums.Role;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.profile.dto.UserProfileDto;
import com.example.ecommerce.profile.service.ProfileService;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getUserProfile_ShouldReturnMappedDto_WhenUserExists() {
        User user = new User();
        user.setId(1L);
        user.setUsername("dogukan");
        user.setEmail("dogukan@example.com");
        user.setFirstName("Dogu");
        user.setLastName("Cicek");
        user.setRoles(Set.of(Role.ROLE_USER));
        when(userRepository.findByUsername("dogukan")).thenReturn(Optional.of(user));

        UserProfileDto result = profileService.getUserProfile("dogukan");

        assertEquals(1L, result.getId());
        assertEquals("dogukan", result.getUsername());
        assertEquals("ROLE_USER", result.getRole());
    }

    @Test
    void getUserProfile_ShouldThrow_WhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> profileService.getUserProfile("missing"));
    }

    @Test
    void updateUserProfile_ShouldUpdateAllowedFieldsOnly() {
        User user = new User();
        user.setId(2L);
        user.setUsername("existing");
        user.setEmail("old@example.com");
        user.setFirstName("Old");
        user.setLastName("Name");
        user.setRoles(Set.of(Role.ROLE_ADMIN));
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(user));

        UserProfileDto request = new UserProfileDto();
        request.setEmail("new@example.com");
        request.setFirstName("New");

        UserProfileDto result = profileService.updateUserProfile("existing", request);

        assertEquals("existing", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_ShouldDelete_WhenUserExists() {
        User user = new User();
        user.setUsername("toDelete");
        when(userRepository.findByUsername("toDelete")).thenReturn(Optional.of(user));

        profileService.deleteUser("toDelete");

        verify(userRepository).delete(user);
    }

    @Test
    void updateUserProfile_ShouldThrow_WhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        UserProfileDto request = new UserProfileDto();
        request.setEmail("new@example.com");

        assertThrows(UsernameNotFoundException.class, () -> profileService.updateUserProfile("missing", request));
    }

    @Test
    void deleteUser_ShouldThrow_WhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> profileService.deleteUser("missing"));
    }
}
