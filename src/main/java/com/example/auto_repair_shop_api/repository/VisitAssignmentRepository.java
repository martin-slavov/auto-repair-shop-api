package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.VisitAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitAssignmentRepository extends JpaRepository<VisitAssignment, Long> {

    List<VisitAssignment> findByMechanicId(Long id);

    Optional<VisitAssignment> findByServiceVisitIdAndMechanicId(Long serviceVisitId, Long mechanicId);

    List<VisitAssignment> findByServiceVisitId(Long serviceVisitId);
}
