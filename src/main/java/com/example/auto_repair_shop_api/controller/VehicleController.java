package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.VehicleResponseDTO;
import com.example.auto_repair_shop_api.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> createVehicle(@Valid @RequestBody VehicleCreateDTO vehicle) {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        VehicleResponseDTO response = vehicleService.createVehicle(vehicle, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> getVehicles() {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        List<VehicleResponseDTO> response = vehicleService.getVehiclesByCurrentUser(currentUsername);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getVehicleById(@PathVariable Long id) {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        VehicleResponseDTO response = vehicleService.getVehicleById(id, currentUsername);

        return ResponseEntity.ok().body(response);
    }
}
