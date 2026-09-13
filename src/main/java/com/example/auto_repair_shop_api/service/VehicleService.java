package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.vehicle.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleResponseDTO;

import java.util.List;

public interface VehicleService {

    VehicleResponseDTO createVehicle(VehicleCreateDTO dto, String currentUsername);

    List<VehicleResponseDTO> getVehiclesByCurrentUser(String currentUsername);

    VehicleResponseDTO getVehicleById(Long id,  String currentUsername);
}
