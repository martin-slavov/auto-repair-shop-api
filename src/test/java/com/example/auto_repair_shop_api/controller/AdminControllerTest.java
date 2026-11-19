package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.config.SecurityConfig;
import com.example.auto_repair_shop_api.dto.user.UserAdminDTO;
import com.example.auto_repair_shop_api.dto.user.UserProfileUpdateDTO;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.AppUserRepository;
import com.example.auto_repair_shop_api.security.JwtUtil;
import com.example.auto_repair_shop_api.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    private static final String VALID_BODY = """
            {"email": "new@example.com", "firstName": "Johnny", "lastName": "Doe"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUserReturnsUpdatedUser() throws Exception {
        when(appUserService.updateUser(eq(5L), any(UserProfileUpdateDTO.class)))
                .thenReturn(new UserAdminDTO(5L, "john", "new@example.com", "Johnny", "Doe", Role.CUSTOMER, true));

        mockMvc.perform(put("/api/admin/users/5").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(appUserService).updateUser(5L, new UserProfileUpdateDTO("new@example.com", "Johnny", "Doe"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUserWithInvalidFieldsReturnsFieldErrors() throws Exception {
        String body = """
                {"email": "", "firstName": "%s", "lastName": " "}
                """.formatted("x".repeat(51));

        mockMvc.perform(put("/api/admin/users/5").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email is required"))
                .andExpect(jsonPath("$.fieldErrors.firstName").value("First name must not exceed 50 characters"))
                .andExpect(jsonPath("$.fieldErrors.lastName").value("Last name is required"));

        verify(appUserService, never()).updateUser(anyLong(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUserWithDuplicateEmailIsConflict() throws Exception {
        when(appUserService.updateUser(eq(5L), any(UserProfileUpdateDTO.class)))
                .thenThrow(new DuplicateResourceException("Email is already in use"));

        mockMvc.perform(put("/api/admin/users/5").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUnknownUserIsNotFound() throws Exception {
        when(appUserService.updateUser(eq(99L), any(UserProfileUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(put("/api/admin/users/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "MECHANIC"})
    void updateUserIsForbiddenForNonAdmins(String role) throws Exception {
        mockMvc.perform(put("/api/admin/users/5").with(user("someone").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());

        verify(appUserService, never()).updateUser(anyLong(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUserWithoutRelatedDataReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/users/5"))
                .andExpect(status().isNoContent());

        verify(appUserService).deleteUser(5L, "admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotDeleteSelf() throws Exception {
        doThrow(new InvalidStateTransitionException("You cannot delete your own account"))
                .when(appUserService).deleteUser(1L, "admin");

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot delete your own account"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUserWithRelatedDataIsConflict() throws Exception {
        String message = "User cannot be deleted because they have related data (vehicles, service requests, " +
                "visits, invoices or mechanic assignments). Disable the user instead.";
        doThrow(new ResourceInUseException(message)).when(appUserService).deleteUser(5L, "admin");

        mockMvc.perform(delete("/api/admin/users/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUnknownUserIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 99"))
                .when(appUserService).deleteUser(99L, "admin");

        mockMvc.perform(delete("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "MECHANIC"})
    void deleteUserIsForbiddenForNonAdmins(String role) throws Exception {
        mockMvc.perform(delete("/api/admin/users/5").with(user("someone").roles(role)))
                .andExpect(status().isForbidden());

        verify(appUserService, never()).deleteUser(anyLong(), anyString());
    }

    @Test
    void deleteUserWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/admin/users/5"))
                .andExpect(status().isUnauthorized());
    }
}
