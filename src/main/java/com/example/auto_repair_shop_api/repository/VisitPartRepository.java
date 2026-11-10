package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.VisitPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitPartRepository extends JpaRepository<VisitPart, Long> {

    List<VisitPart> findByServiceVisitId(Long visitId);

    boolean existsByPartId(Long partId);

    boolean existsByServiceVisitId(Long serviceVisitId);

    Optional<VisitPart> findByIdAndServiceVisitId(Long id, Long serviceVisitId);
}
