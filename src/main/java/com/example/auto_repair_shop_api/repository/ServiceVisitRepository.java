package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.ServiceVisit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceVisitRepository extends JpaRepository<ServiceVisit, Long> {
}
