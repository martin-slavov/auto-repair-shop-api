package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.vehicle.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleResponseDTO;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.VehicleMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.VehicleRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final AppUserService appUserService;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Autowired
    public VehicleServiceImpl(AppUserService appUserService, VehicleRepository vehicleRepository, VehicleMapper vehicleMapper) {
        this.appUserService = appUserService;
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    @Transactional
    public VehicleResponseDTO createVehicle(VehicleCreateDTO dto, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (vehicleRepository.existsByLicensePlate(dto.licensePlate())) {
            throw new DuplicateResourceException("Vehicle with this license plate already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(dto.licensePlate());
        vehicle.setManufacturer(dto.manufacturer());
        vehicle.setModel(dto.model());
        vehicle.setYear(dto.year());
        vehicle.setOwner(appUser);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return vehicleMapper.toResponseDto(savedVehicle);
    }

    @Override
    public List<VehicleResponseDTO> getVehiclesByCurrentUser(String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        List<Vehicle> vehicles;
        if (appUser.getRole() == Role.ADMIN) {
            vehicles = vehicleRepository.findAll();
        } else {
            vehicles = vehicleRepository.findByOwnerId(appUser.getId());
        }

        return vehicles.stream()
                .map(vehicleMapper::toResponseDto)
                .toList();
    }

    @Override
    public VehicleResponseDTO getVehicleById(Long id, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        boolean isOwner = vehicle.getOwner().getId().equals(appUser.getId());
        boolean isAdmin = appUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new OwnershipViolationException("You do not have permission to view this vehicle");
        }

        return vehicleMapper.toResponseDto(vehicle);
    }
}
