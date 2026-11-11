package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.vehicle.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleResponseDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleUpdateDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.service.VehicleService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Search vehicles (paged). Customers see only their own vehicles; admins see all " +
            "and can filter by 'ownerId'.")
    @GetMapping
    public ResponseEntity<PageResponse<VehicleResponseDTO>> getVehicles(
            @Parameter(description = "License plate contains (spaces ignored, case-insensitive)")
            @RequestParam(required = false) String licensePlate,
            @Parameter(description = "Manufacturer contains (case-insensitive)")
            @RequestParam(required = false) String manufacturer,
            @Parameter(description = "Model contains (case-insensitive)")
            @RequestParam(required = false) String model,
            @Parameter(description = "Minimum year (inclusive)")
            @RequestParam(required = false) Integer yearFrom,
            @Parameter(description = "Maximum year (inclusive)")
            @RequestParam(required = false) Integer yearTo,
            @Parameter(description = "ADMIN only: filter by owner ID. Ignored for other users.")
            @RequestParam(required = false) Long ownerId,
            @Parameter(description = "Page number, 0-based (default 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 10, max 50)")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "field,asc|desc - fields: licensePlate, manufacturer, model, year, " +
                    "firstRegistrationDate (default licensePlate,asc)")
            @RequestParam(required = false) String sort) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        PageResponse<VehicleResponseDTO> response = vehicleService.getVehiclesByCurrentUser(currentUsername,
                licensePlate, manufacturer, model, yearFrom, yearTo, ownerId, page, size, sort);

        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Get a specific vehicle by ID")
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getVehicleById(@PathVariable Long id) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VehicleResponseDTO response = vehicleService.getVehicleById(id, currentUsername);

        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Update a vehicle (owner or admin)")
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> updateVehicle(@PathVariable Long id, @Valid @RequestBody VehicleUpdateDTO vehicle) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VehicleResponseDTO response = vehicleService.updateVehicle(id, vehicle, currentUsername);

        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Delete a vehicle (owner or admin); fails if it has service requests or visits")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        vehicleService.deleteVehicle(id, currentUsername);

        return ResponseEntity.noContent().build();
    }
}
