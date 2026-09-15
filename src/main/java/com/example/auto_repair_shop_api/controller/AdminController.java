package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.auth.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.auth.RegisterResponseDTO;
import com.example.auto_repair_shop_api.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
