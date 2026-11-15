package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.auth.LoginRequestDTO;
import com.example.auto_repair_shop_api.dto.auth.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.auth.RegisterResponseDTO;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Authentication", description = "User registration and login")
@SecurityRequirements()
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService appUserService;
    private final AuthService authService;

    public AuthController(AppUserService appUserService, AuthService authService) {
        this.appUserService = appUserService;
        this.authService = authService;
    }

    @Operation(summary = "Register a customer account", description = "Access: public.")
    @ApiResponse(responseCode = "201", description = "Customer account created",
            content = @Content(schema = @Schema(implementation = RegisterResponseDTO.class)))
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> registerCustomer(@Valid @RequestBody RegisterRequestDTO dto) {
        RegisterResponseDTO response = appUserService.registerCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Log in and receive a JWT token",
            description = "Access: public. Paste the returned token into Authorize to call the other endpoints.")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequestDTO dto) {
        String token = authService.login(dto);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
