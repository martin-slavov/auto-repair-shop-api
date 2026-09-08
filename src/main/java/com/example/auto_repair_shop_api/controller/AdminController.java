package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.RegisterResponseDTO;
import com.example.auto_repair_shop_api.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/admin")
public class AdminController {

    private final AppUserService appUserService;

    public AdminController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/users/mechanic")
    public ResponseEntity<RegisterResponseDTO> createMechanic(@Valid @RequestBody RegisterRequestDTO dto) {

        RegisterResponseDTO response = appUserService.createMechanic(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
