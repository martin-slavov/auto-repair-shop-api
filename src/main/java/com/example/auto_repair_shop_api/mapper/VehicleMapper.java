package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.VehicleResponseDTO;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public VehicleResponseDTO toResponseDto(Vehicle vehicle) {

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
