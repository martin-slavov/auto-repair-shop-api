package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.user.UserAdminDTO;
import com.example.auto_repair_shop_api.dto.user.UserProfileDTO;
import com.example.auto_repair_shop_api.dto.user.UserProfileUpdateDTO;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.AppUserMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.AppUserRepository;
import com.example.auto_repair_shop_api.repository.VehicleRepository;
import com.example.auto_repair_shop_api.repository.VisitAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VisitAssignmentRepository visitAssignmentRepository;

    private AppUserServiceImpl appUserService;

    private AppUser customer;
    private AppUser admin;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserServiceImpl(appUserRepository, passwordEncoder, new AppUserMapper(),
                vehicleRepository, visitAssignmentRepository);

        customer = user(1L, "john", Role.CUSTOMER);
        admin = user(2L, "admin", Role.ADMIN);
    }

    @Test
    void getProfileReturnsCurrentUser() {
        when(appUserRepository.findByUsername("john")).thenReturn(Optional.of(customer));

        UserProfileDTO result = appUserService.getProfile("john");

        assertThat(result).isEqualTo(new UserProfileDTO(1L, "john", "john@example.com", "First", "Last",
                Role.CUSTOMER, true));
    }

    @Test
    void updateProfileChangesEditableFieldsOnly() {
        when(appUserRepository.findByUsername("john")).thenReturn(Optional.of(customer));
        when(appUserRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);

        UserProfileDTO result = appUserService.updateProfile("john",
                new UserProfileUpdateDTO("new@example.com", "Johnny", "Doe"));

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.firstName()).isEqualTo("Johnny");
        assertThat(result.lastName()).isEqualTo("Doe");
        assertThat(result.username()).isEqualTo("john");
        assertThat(result.role()).isEqualTo(Role.CUSTOMER);
        assertThat(customer.getUsername()).isEqualTo("john");
        assertThat(customer.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void updateProfileKeepingOwnEmailIsAllowed() {
        when(appUserRepository.findByUsername("john")).thenReturn(Optional.of(customer));
        when(appUserRepository.existsByEmailAndIdNot("john@example.com", 1L)).thenReturn(false);

        UserProfileDTO result = appUserService.updateProfile("john",
                new UserProfileUpdateDTO("john@example.com", "Johnny", "Last"));

        assertThat(result.email()).isEqualTo("john@example.com");
        assertThat(result.firstName()).isEqualTo("Johnny");
    }

    @Test
    void updateProfileWithEmailOfAnotherUserIsRejected() {
        when(appUserRepository.findByUsername("john")).thenReturn(Optional.of(customer));
        when(appUserRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> appUserService.updateProfile("john",
                new UserProfileUpdateDTO("taken@example.com", "Johnny", "Doe")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email is already in use");

        assertThat(customer.getEmail()).isEqualTo("john@example.com");
        assertThat(customer.getFirstName()).isEqualTo("First");
    }

    @Test
    void adminUpdatesAnotherUser() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(appUserRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);

        UserAdminDTO result = appUserService.updateUser(1L,
                new UserProfileUpdateDTO("new@example.com", "Johnny", "Doe"));

        assertThat(result).isEqualTo(new UserAdminDTO(1L, "john", "new@example.com", "Johnny", "Doe",
                Role.CUSTOMER, true));
    }

    @Test
    void adminUpdateOfUnknownUserIsNotFound() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.updateUser(99L,
                new UserProfileUpdateDTO("new@example.com", "Johnny", "Doe")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminUpdateWithDuplicateEmailIsRejected() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(appUserRepository.existsByEmailAndIdNot("admin@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> appUserService.updateUser(1L,
                new UserProfileUpdateDTO("admin@example.com", "Johnny", "Doe")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void adminCannotDeleteOwnAccount() {
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> appUserService.deleteUser(2L, "admin"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("You cannot delete your own account");

        verify(appUserRepository, never()).delete(any(AppUser.class));
    }

    @Test
    void userWithVehiclesCannotBeDeleted() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(vehicleRepository.existsByOwnerId(1L)).thenReturn(true);

        assertThatThrownBy(() -> appUserService.deleteUser(1L, "admin"))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("Disable the user instead");

        verify(appUserRepository, never()).delete(any(AppUser.class));
    }

    @Test
    void mechanicWithAssignmentsCannotBeDeleted() {
        AppUser mechanic = user(3L, "mech", Role.MECHANIC);
        when(appUserRepository.findById(3L)).thenReturn(Optional.of(mechanic));
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(vehicleRepository.existsByOwnerId(3L)).thenReturn(false);
        when(visitAssignmentRepository.existsByMechanicId(3L)).thenReturn(true);

        assertThatThrownBy(() -> appUserService.deleteUser(3L, "admin"))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("Disable the user instead");

        verify(appUserRepository, never()).delete(any(AppUser.class));
    }

    @Test
    void userWithoutRelatedDataIsDeleted() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(vehicleRepository.existsByOwnerId(1L)).thenReturn(false);
        when(visitAssignmentRepository.existsByMechanicId(1L)).thenReturn(false);

        appUserService.deleteUser(1L, "admin");

        verify(appUserRepository).delete(customer);
    }

    @Test
    void deleteOfUnknownUserIsNotFound() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.deleteUser(99L, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private AppUser user(Long id, String username, Role role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
