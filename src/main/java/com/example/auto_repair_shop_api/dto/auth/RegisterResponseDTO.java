package com.example.auto_repair_shop_api.dto.auth;

import com.example.auto_repair_shop_api.model.enums.Role;

public record RegisterResponseDTO(Long id, String username, String email, Role role) {

}
