package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.auth.RegisterResponseDTO;
import com.example.auto_repair_shop_api.dto.user.UserAdminDTO;
import com.example.auto_repair_shop_api.dto.user.UserProfileDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class AppUserMapper {

    public RegisterResponseDTO toResponseDto(AppUser appUser) {
        return new RegisterResponseDTO(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getEmail(),
                appUser.getRole()
        );
    }

    public UserAdminDTO toAdminDto(AppUser appUser) {
        return new UserAdminDTO(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getEmail(),
                appUser.getFirstName(),
                appUser.getLastName(),
                appUser.getRole(),
                appUser.isEnabled()
        );
    }

    public UserProfileDTO toProfileDto(AppUser appUser) {
        return new UserProfileDTO(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getEmail(),
                appUser.getFirstName(),
                appUser.getLastName(),
                appUser.getRole(),
                appUser.isEnabled()
        );
    }
}
