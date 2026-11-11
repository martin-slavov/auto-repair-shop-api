package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {

    @Override
    @EntityGraph(attributePaths = "vehicle")
    Page<ServiceRequest> findAll(Specification<ServiceRequest> spec, Pageable pageable);

    List<ServiceRequest> findByVehicleOwnerId(Long ownerId);

    List<ServiceRequest> findByStatus(RequestStatus status);

    boolean existsByVehicleId(Long vehicleId);
}
