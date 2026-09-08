package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.VehicleResponseDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public VehicleResponseDTO createVehicle(VehicleCreateDTO dto, String currentUsername) {

        AppUser appUser = appUserService.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + currentUsername));

        if (vehicleRepository.existsByLicensePlate(dto.getLicensePlate())) {
            throw new IllegalArgumentException("Vehicle with this license plate already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(dto.getLicensePlate());
        vehicle.setManufacturer(dto.getManufacturer());
        vehicle.setModel(dto.getModel());
        vehicle.setYear(dto.getYear());
        vehicle.setOwner(appUser);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return toResponseDto(savedVehicle);
    }

    @Override
    public List<VehicleResponseDTO> getVehiclesByCurrentUser(String currentUsername) {
        AppUser appUser = appUserService.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + currentUsername));

        List<Vehicle> vehicles;
        if (appUser.getRole() == Role.ADMIN) {
            vehicles = vehicleRepository.findAll();
        } else {
            vehicles = vehicleRepository.findByOwnerId(appUser.getId());
        }

        return vehicles.stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public VehicleResponseDTO getVehicleById(Long id, String currentUsername) {
        AppUser appUser = appUserService.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + currentUsername));

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        boolean isOwner = vehicle.getOwner().getId().equals(appUser.getId());
        boolean isAdmin = appUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new SecurityException("You do not have permission to view this vehicle");
        }

        return toResponseDto(vehicle);
    }

    private VehicleResponseDTO toResponseDto(Vehicle vehicle) {
        AppUser owner = vehicle.getOwner();
        String ownerName = owner.getFirstName() + " " + owner.getLastName();

        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getManufacturer(),
                vehicle.getModel(),
                vehicle.getYear(),
                owner.getId(),
                ownerName
        );
    }
}
