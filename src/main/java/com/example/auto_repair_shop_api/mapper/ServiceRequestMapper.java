package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.dto.ServiceRequestResponseDTO;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestMapper {

    public ServiceRequestResponseDTO toResponseDto(ServiceRequest request) {
        return new ServiceRequestResponseDTO(
                request.getId(),
                request.getDescription(),
                request.getStatus().name(),
                request.getCreatedAt(),
                request.getVehicle().getLicensePlate()
        );
    }
}
