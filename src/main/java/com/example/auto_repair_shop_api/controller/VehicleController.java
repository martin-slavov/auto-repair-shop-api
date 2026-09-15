package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.vehicle.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleResponseDTO;
import com.example.auto_repair_shop_api.service.VehicleService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Vehicles", description = "Manage customer vehicles")
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "Create a new vehicle for the current customer")
    @PostMapping
    public ResponseEntity<VehicleResponseDTO> createVehicle(@Valid @RequestBody VehicleCreateDTO vehicle) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VehicleResponseDTO response = vehicleService.createVehicle(vehicle, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all vehicles for the current user (own vehicles for customers, all for admins)")
    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> getVehicles() {

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<VehicleResponseDTO> response = vehicleService.getVehiclesByCurrentUser(currentUsername);

        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Get a specific vehicle by ID")
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getVehicleById(@PathVariable Long id) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VehicleResponseDTO response = vehicleService.getVehicleById(id, currentUsername);

        return ResponseEntity.ok().body(response);
    }
}
