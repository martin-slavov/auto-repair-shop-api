package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.Part;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part, Long> {

    boolean existsBySerialNumber(String serialNumber);
}
