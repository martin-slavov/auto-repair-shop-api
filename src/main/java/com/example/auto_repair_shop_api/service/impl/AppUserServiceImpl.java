package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.auth.RegisterRequestDTO;
import com.example.auto_repair_shop_api.dto.auth.RegisterResponseDTO;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.AppUserMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.AppUserRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AppUserServiceImpl implements AppUserService {

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
    public Optional<AppUser> findByUsername(String username) {
        return appUserRepository.findByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        AppUser appUser = getByUsernameOrThrow(username);

        return new User(
                appUser.getUsername(),
                appUser.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())));
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
    public AppUser getByUsernameOrThrow(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Override
    public AppUser getByIdOrThrow(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
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
