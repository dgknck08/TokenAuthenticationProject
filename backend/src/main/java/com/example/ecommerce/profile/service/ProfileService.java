package com.example.ecommerce.profile.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.profile.dto.UserProfileDto;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return mapToDto(user);
    }

    @Transactional
    public UserProfileDto updateUserProfile(String username, UserProfileDto updatedProfile) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Sadece e-posta ve diğer izin verilen alanlar güncelleniyor
        if (updatedProfile.getEmail() != null) {
            user.setEmail(updatedProfile.getEmail());
        }
        if (updatedProfile.getFirstName() != null) {
            user.setFirstName(updatedProfile.getFirstName());
        }
        if (updatedProfile.getLastName() != null) {
            user.setLastName(updatedProfile.getLastName());
        }
        // username ve password gibi kritik alanlar güncellenmiyor bu metotta

        userRepository.save(user);
        return mapToDto(user);
    }

    @Transactional
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        userRepository.delete(user);
    }

    private UserProfileDto mapToDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        List<String> roleNames = user.getRoles().stream()
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .toList();
        String roleName = roleNames.stream()
                .filter(role -> "ROLE_ADMIN".equals(role))
                .findFirst()
                .orElse(roleNames.stream().findFirst().orElse("NO_ROLE"));
        dto.setRole(roleName);
        dto.setRoles(roleNames);
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        return dto;
    }


}

