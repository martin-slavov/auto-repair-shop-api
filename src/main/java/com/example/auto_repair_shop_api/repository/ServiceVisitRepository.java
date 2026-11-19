package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ServiceVisitRepository extends JpaRepository<ServiceVisit, Long>, JpaSpecificationExecutor<ServiceVisit> {

    @Override
    @EntityGraph(attributePaths = "vehicle")
    Page<ServiceVisit> findAll(Specification<ServiceVisit> spec, Pageable pageable);

    boolean existsByVehicleId(Long vehicleId);

    /**
     * Visits in one of the given statuses that start strictly between {@code from} and {@code to} and have at
     * least one of the given mechanics assigned (lead or assistant). Used for the booking conflict check and for
     * the free-slots view, so a whole day needs one query.
     */
    @Query("""
            select v from ServiceVisit v
            where v.status in :statuses
              and v.scheduledDate > :from
              and v.scheduledDate < :to
              and exists (select a.id from VisitAssignment a
                          where a.serviceVisit = v and a.mechanic.id in :mechanicIds)
            """)
    List<ServiceVisit> findByMechanicsAndStatusStartingBetween(@Param("mechanicIds") Collection<Long> mechanicIds,
                                                               @Param("statuses") Collection<VisitStatus> statuses,
                                                               @Param("from") LocalDateTime from,
                                                               @Param("to") LocalDateTime to);
}
