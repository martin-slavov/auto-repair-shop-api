package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByVehicleOwnerId(Long ownerId);

    List<ServiceRequest> findByStatus(RequestStatus status);
}
