package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    // Load the owner in the same query: the mapper reads the owner's name for every row.
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Vehicle> findAll(Specification<Vehicle> spec, Pageable pageable);

    List<Vehicle> findByOwnerId(Long ownerId);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);
}
