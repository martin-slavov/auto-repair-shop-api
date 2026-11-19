package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.config.SecurityConfig;
import com.example.auto_repair_shop_api.dto.user.UserProfileDTO;
import com.example.auto_repair_shop_api.dto.user.UserProfileUpdateDTO;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.AppUserRepository;
import com.example.auto_repair_shop_api.security.JwtUtil;
import com.example.auto_repair_shop_api.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

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
    @WithMockUser(username = "john", roles = "CUSTOMER")
    void getProfileReturnsCurrentUser() throws Exception {
        when(appUserService.getProfile("john")).thenReturn(profile("john@example.com", "First", "Last"));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("First"))
                .andExpect(jsonPath("$.lastName").value("Last"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(username = "mech", roles = "MECHANIC")
    void getProfileWorksForEveryRole() throws Exception {
        when(appUserService.getProfile("mech")).thenReturn(profile("mech@example.com", "First", "Last"));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    void getProfileWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "john", roles = "CUSTOMER")
    void updateProfileReturnsUpdatedUser() throws Exception {
        when(appUserService.updateProfile(eq("john"), any(UserProfileUpdateDTO.class)))
                .thenReturn(profile("new@example.com", "Johnny", "Doe"));

        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.firstName").value("Johnny"));

        verify(appUserService).updateProfile("john", new UserProfileUpdateDTO("new@example.com", "Johnny", "Doe"));
    }

    @Test
    @WithMockUser(username = "john", roles = "CUSTOMER")
    void updateProfileWithInvalidFieldsReturnsFieldErrors() throws Exception {
        String body = """
                {"email": "not-an-email", "firstName": "", "lastName": "%s"}
                """.formatted("x".repeat(51));

        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email should be valid"))
                .andExpect(jsonPath("$.fieldErrors.firstName").value("First name is required"))
                .andExpect(jsonPath("$.fieldErrors.lastName").value("Last name must not exceed 50 characters"));

        verify(appUserService, never()).updateProfile(anyString(), any());
    }

    @Test
    @WithMockUser(username = "john", roles = "CUSTOMER")
    void updateProfileWithTooLongEmailIsRejected() throws Exception {
        String body = """
                {"email": "%s@example.com", "firstName": "Johnny", "lastName": "Doe"}
                """.formatted("a".repeat(250));

        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @WithMockUser(username = "john", roles = "CUSTOMER")
    void updateProfileWithDuplicateEmailIsConflict() throws Exception {
        when(appUserService.updateProfile(eq("john"), any(UserProfileUpdateDTO.class)))
                .thenThrow(new DuplicateResourceException("Email is already in use"));

        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    void updateProfileWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    private UserProfileDTO profile(String email, String firstName, String lastName) {
        return new UserProfileDTO(1L, "john", email, firstName, lastName, Role.CUSTOMER, true);
    }
}
