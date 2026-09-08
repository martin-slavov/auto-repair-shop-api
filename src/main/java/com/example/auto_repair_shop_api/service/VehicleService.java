package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.VehicleResponseDTO;

import java.util.List;

public interface VehicleService {

    VehicleResponseDTO createVehicle(VehicleCreateDTO dto, String currentUsername);

    List<VehicleResponseDTO> getVehiclesByCurrentUser(String currentUsername);

    VehicleResponseDTO getVehicleById(Long id,  String currentUsername);
}
