package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.auth.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.auth.RegisterResponseDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.user.UpdateUserEnabledDTO;
import com.example.auto_repair_shop_api.dto.user.UserAdminDTO;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "Admin-only user management")
@RestController()
@RequestMapping("/api/admin")
public class AdminController {

    private final AppUserService appUserService;

    public AdminController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Operation(summary = "Create a new mechanic account (admin only)")
    @PostMapping("/users/mechanic")
    public ResponseEntity<RegisterResponseDTO> createMechanic(@Valid @RequestBody RegisterRequestDTO dto) {

        RegisterResponseDTO response = appUserService.createMechanic(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Create a new admin account (admin only)")
    @PostMapping("/users/admin")
    public ResponseEntity<RegisterResponseDTO> createAdmin(@Valid @RequestBody RegisterRequestDTO dto) {

        RegisterResponseDTO response = appUserService.createAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Search users (paged, admin only). Roles are fixed at creation.")
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserAdminDTO>> getUsers(
            @Parameter(description = "Username contains (case-insensitive)")
            @RequestParam(required = false) String username,
            @Parameter(description = "Email contains (case-insensitive)")
            @RequestParam(required = false) String email,
            @Parameter(description = "Filter by role")
            @RequestParam(required = false) Role role,
            @Parameter(description = "Filter by enabled flag")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Page number, 0-based (default 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 10, max 50)")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "field,asc|desc - fields: username, email, role, enabled, id " +
                    "(default username,asc)")
            @RequestParam(required = false) String sort) {

        PageResponse<UserAdminDTO> response = appUserService.searchUsers(username, email, role, enabled,
                page, size, sort);

        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Enable or disable a user; you cannot disable yourself or the last enabled admin")
    @PatchMapping("/users/{id}/enabled")
    public ResponseEntity<UserAdminDTO> updateEnabled(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateUserEnabledDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        UserAdminDTO response = appUserService.updateEnabled(id, dto, currentUsername);

        return ResponseEntity.ok().body(response);
    }
}
