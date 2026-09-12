package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.RegisterResponseDTO;
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
}
