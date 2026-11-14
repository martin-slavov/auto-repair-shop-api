package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.auth.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.auth.RegisterResponseDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.user.UpdateUserEnabledDTO;
import com.example.auto_repair_shop_api.dto.user.UserAdminDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.enums.Role;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AppUserService extends UserDetailsService {

    RegisterResponseDTO registerCustomer(RegisterRequestDTO dto);

    RegisterResponseDTO createMechanic(RegisterRequestDTO dto);

    RegisterResponseDTO createAdmin(RegisterRequestDTO dto);

    AppUser getByUsernameOrThrow(String username);

    AppUser getByIdOrThrow(Long id);

    PageResponse<UserAdminDTO> searchUsers(String username, String email, Role role, Boolean enabled,
                                           Integer page, Integer size, String sort);

    UserAdminDTO updateEnabled(Long id, UpdateUserEnabledDTO dto, String currentUsername);
}
