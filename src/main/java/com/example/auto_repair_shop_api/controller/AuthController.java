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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> registerCustomer(@Valid @RequestBody RegisterRequestDTO dto) {

        RegisterResponseDTO response = appUserService.registerCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
