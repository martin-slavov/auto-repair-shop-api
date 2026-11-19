package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.user.ChangePasswordDTO;
import com.example.auto_repair_shop_api.dto.user.UserProfileDTO;
import com.example.auto_repair_shop_api.dto.user.UserProfileUpdateDTO;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "Current user's own account")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserService appUserService;

    public UserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Operation(summary = "Get your own profile", description = "Access: any authenticated user.")
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getProfile() {

        String currentUsername = SecurityUtils.getCurrentUsername();
        return ResponseEntity.ok(appUserService.getProfile(currentUsername));
    }

    @Operation(summary = "Update your own profile",
            description = "Access: any authenticated user. Updates email, first name and last name. " +
                    "Username and role never change, so the current token stays valid.")
    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        return ResponseEntity.ok(appUserService.updateProfile(currentUsername, dto));
    }

    @Operation(summary = "Change your own password",
            description = "Access: any authenticated user. Requires the current password. " +
                    "Tokens issued before the change stay valid until they expire.")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        appUserService.changePassword(currentUsername, dto);

        return ResponseEntity.noContent().build();
    }
}
