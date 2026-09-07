package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.RegisterResponseDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

public interface AppUserService extends UserDetailsService {

    Optional<AppUser> findByUsername(String username);
    RegisterResponseDTO registerCustomer(RegisterRequestDTO dto);
}
