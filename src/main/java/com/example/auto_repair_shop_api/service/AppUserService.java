package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.auth.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.auth.RegisterResponseDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AppUserService extends UserDetailsService {

    RegisterResponseDTO registerCustomer(RegisterRequestDTO dto);

    RegisterResponseDTO createMechanic(RegisterRequestDTO dto);

    AppUser getByUsernameOrThrow(String username);

    AppUser getByIdOrThrow(Long id);
}
