package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.vehicle.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleResponseDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleUpdateDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;

public interface VehicleService {

    VehicleResponseDTO createVehicle(VehicleCreateDTO dto, String currentUsername);

    PageResponse<VehicleResponseDTO> getVehiclesByCurrentUser(String currentUsername,
                                                              String licensePlate, String manufacturer, String model,
                                                              Integer yearFrom, Integer yearTo, Long ownerId,
                                                              Integer page, Integer size, String sort);

    VehicleResponseDTO getVehicleById(Long id,  String currentUsername);

    VehicleResponseDTO updateVehicle(Long id, VehicleUpdateDTO dto, String currentUsername);

    void deleteVehicle(Long id, String currentUsername);
}
