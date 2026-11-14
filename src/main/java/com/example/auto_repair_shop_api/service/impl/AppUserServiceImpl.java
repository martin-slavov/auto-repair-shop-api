package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.auth.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.auth.RegisterResponseDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.user.UpdateUserEnabledDTO;
import com.example.auto_repair_shop_api.dto.user.UserAdminDTO;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.AppUserMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.AppUserRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.util.PageRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.auto_repair_shop_api.repository.specification.AppUserSpecifications.emailContains;
import static com.example.auto_repair_shop_api.repository.specification.AppUserSpecifications.hasRole;
import static com.example.auto_repair_shop_api.repository.specification.AppUserSpecifications.isEnabled;
import static com.example.auto_repair_shop_api.repository.specification.AppUserSpecifications.usernameContains;

@Service
public class AppUserServiceImpl implements AppUserService {

    // Sort whitelist: API field name -> entity property.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "username", "username",
            "email", "email",
            "role", "role",
            "enabled", "enabled",
            "id", "id"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "username");

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper appUserMapper;

    @Autowired
    public AppUserServiceImpl(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, AppUserMapper appUserMapper) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.appUserMapper = appUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        AppUser appUser = getByUsernameOrThrow(username);

        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()))
                .disabled(!appUser.isEnabled())
                .build();
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerCustomer(RegisterRequestDTO dto) {
        return createUserWithRole(dto, Role.CUSTOMER);
    }

    @Override
    @Transactional
    public RegisterResponseDTO createMechanic(RegisterRequestDTO dto) {
        return createUserWithRole(dto, Role.MECHANIC);
    }

    @Override
    @Transactional
    public RegisterResponseDTO createAdmin(RegisterRequestDTO dto) {
        return createUserWithRole(dto, Role.ADMIN);
    }

    @Override
    public AppUser getByUsernameOrThrow(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Override
    public AppUser getByIdOrThrow(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public PageResponse<UserAdminDTO> searchUsers(String username, String email, Role role, Boolean enabled,
                                                  Integer page, Integer size, String sort) {

        PageRequest pageRequest = PageRequestFactory.create(page, size, sort, SORT_FIELDS, DEFAULT_SORT);

        List<Specification<AppUser>> filters = new ArrayList<>();
        if (username != null && !username.isBlank()) {
            filters.add(usernameContains(username));
        }
        if (email != null && !email.isBlank()) {
            filters.add(emailContains(email));
        }
        if (role != null) {
            filters.add(hasRole(role));
        }
        if (enabled != null) {
            filters.add(isEnabled(enabled));
        }

        Page<AppUser> result = appUserRepository.findAll(Specification.allOf(filters), pageRequest);
        return PageResponse.from(result, appUserMapper::toAdminDto);
    }

    @Override
    @Transactional
    public UserAdminDTO updateEnabled(Long id, UpdateUserEnabledDTO dto, String currentUsername) {
        AppUser target = getByIdOrThrow(id);
        boolean enabled = dto.enabled();

        if (target.isEnabled() == enabled) {
            return appUserMapper.toAdminDto(target);
        }

        if (!enabled) {
            if (isSameUser(target, currentUsername)) {
                throw new InvalidStateTransitionException("You cannot disable your own account");
            }
            if (isEnabledAdmin(target)) {
                ensureNotLastEnabledAdmin();
            }
        }

        target.setEnabled(enabled);
        return appUserMapper.toAdminDto(target);
    }

    private boolean isSameUser(AppUser target, String currentUsername) {
        return target.getId().equals(getByUsernameOrThrow(currentUsername).getId());
    }

    private boolean isEnabledAdmin(AppUser user) {
        return user.getRole() == Role.ADMIN && user.isEnabled();
    }

    private void ensureNotLastEnabledAdmin() {
        if (appUserRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new ResourceInUseException("Cannot disable the last enabled admin");
        }
    }

    private RegisterResponseDTO createUserWithRole(RegisterRequestDTO dto, Role role) {
        if (appUserRepository.existsByUsername(dto.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (appUserRepository.existsByEmail(dto.email())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        AppUser appUser = new AppUser();
        appUser.setUsername(dto.username());
        appUser.setPasswordHash(passwordEncoder.encode(dto.password()));
        appUser.setEmail(dto.email());
        appUser.setFirstName(dto.firstName());
        appUser.setLastName(dto.lastName());
        appUser.setRole(role);

        AppUser savedUser = appUserRepository.save(appUser);
        return appUserMapper.toResponseDto(savedUser);
    }
}
