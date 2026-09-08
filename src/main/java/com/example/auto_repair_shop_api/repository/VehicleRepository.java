package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerId(Long ownerId);

    boolean existsByLicensePlate(String licensePlate);
}
