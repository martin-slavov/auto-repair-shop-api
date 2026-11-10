package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.vehicle.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleResponseDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleUpdateDTO;

import java.util.List;

public interface VehicleService {

    VehicleResponseDTO createVehicle(VehicleCreateDTO dto, String currentUsername);

    List<VehicleResponseDTO> getVehiclesByCurrentUser(String currentUsername);

    VehicleResponseDTO getVehicleById(Long id,  String currentUsername);

    VehicleResponseDTO updateVehicle(Long id, VehicleUpdateDTO dto, String currentUsername);

    void deleteVehicle(Long id, String currentUsername);
}
