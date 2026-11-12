package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.ServiceVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceVisitRepository extends JpaRepository<ServiceVisit, Long>, JpaSpecificationExecutor<ServiceVisit> {

    @Override
    @EntityGraph(attributePaths = "vehicle")
    Page<ServiceVisit> findAll(Specification<ServiceVisit> spec, Pageable pageable);

    boolean existsByVehicleId(Long vehicleId);
}
