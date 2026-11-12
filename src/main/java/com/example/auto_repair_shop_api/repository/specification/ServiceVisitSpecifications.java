package com.example.auto_repair_shop_api.repository.specification;

import com.example.auto_repair_shop_api.model.ServiceVisit;
import com.example.auto_repair_shop_api.model.VisitAssignment;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.LIKE_ESCAPE;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.licensePlateContainsPattern;

/**
 * Reusable filter pieces for ServiceVisit searches. The service always adds the role scope first
 * (assignedToMechanic(...) for mechanics, vehicleOwnedBy(...) for customers) and only adds the other
 * pieces when their query parameter was sent.
 */
public final class ServiceVisitSpecifications {

    private ServiceVisitSpecifications() {
    }

    /** Ownership scope: the visit's vehicle belongs to this user. Compares the owner FK on the vehicle row. */
    public static Specification<ServiceVisit> vehicleOwnedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("vehicle").get("owner").get("id"), ownerId);
    }

    /**
     * An assignment exists for this mechanic on the visit. Uses a correlated EXISTS subquery
     * (ServiceVisit has no mapped assignments collection), so a visit is never repeated per mechanic.
     */
    public static Specification<ServiceVisit> assignedToMechanic(Long mechanicId) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<VisitAssignment> assignment = sub.from(VisitAssignment.class);
            sub.select(assignment.get("id")).where(
                    cb.equal(assignment.get("serviceVisit"), root),
                    cb.equal(assignment.get("mechanic").get("id"), mechanicId));
            return cb.exists(sub);
        };
    }

    public static Specification<ServiceVisit> hasStatus(VisitStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Date part of scheduledDate >= from (inclusive): scheduledDate >= from 00:00. */
    public static Specification<ServiceVisit> scheduledOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("scheduledDate"), from.atStartOfDay());
    }

    /** Date part of scheduledDate <= to (inclusive): scheduledDate < (to + 1 day) 00:00. */
    public static Specification<ServiceVisit> scheduledOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThan(root.get("scheduledDate"), to.plusDays(1).atStartOfDay());
    }

    /** Case-insensitive "contains" on the vehicle's plate; the text is normalized like stored plates first. */
    public static Specification<ServiceVisit> licensePlateContains(String licensePlate) {
        String pattern = licensePlateContainsPattern(licensePlate);
        return (root, query, cb) -> cb.like(cb.lower(root.get("vehicle").get("licensePlate")), pattern, LIKE_ESCAPE);
    }

    /** Equals on the vehicle FK. Still combined with the role scope by the service. */
    public static Specification<ServiceVisit> hasVehicleId(Long vehicleId) {
        return (root, query, cb) -> cb.equal(root.get("vehicle").get("id"), vehicleId);
    }
}
