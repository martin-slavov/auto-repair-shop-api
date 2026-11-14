package com.example.auto_repair_shop_api.dto.user;

import com.example.auto_repair_shop_api.model.enums.Role;

/**
 * User as seen by an admin. Never carries the password hash.
 */
public record UserAdminDTO(Long id, String username, String email, String firstName, String lastName,
                           Role role, boolean enabled) {
}
