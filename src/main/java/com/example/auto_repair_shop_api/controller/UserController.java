package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.user.ChangePasswordDTO;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @Operation(summary = "Change the current user's own password (requires the current password). " +
            "Tokens issued before the change stay valid until they expire.")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        appUserService.changePassword(currentUsername, dto);

        return ResponseEntity.noContent().build();
    }
}
