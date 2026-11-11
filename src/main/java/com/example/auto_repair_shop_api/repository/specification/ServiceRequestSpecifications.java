package com.example.auto_repair_shop_api.repository.specification;

import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.enums.RequestStatus;
import com.example.auto_repair_shop_api.model.enums.RequestType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.LIKE_ESCAPE;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.licensePlateContainsPattern;

/**
 * Reusable filter pieces for ServiceRequest searches. The service always adds vehicleOwnedBy(...) first
 * for non-admins and only adds the other pieces when their query parameter was sent.
 */
public final class ServiceRequestSpecifications {

    private ServiceRequestSpecifications() {
    }

    /** Ownership scope: the request's vehicle belongs to this user. Compares the owner FK on the vehicle row. */
    public static Specification<ServiceRequest> vehicleOwnedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("vehicle").get("owner").get("id"), ownerId);
    }

    public static Specification<ServiceRequest> hasStatus(RequestStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ServiceRequest> hasType(RequestType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    /** preferredDate >= from (inclusive). Requests without a preferred date never match. */
    public static Specification<ServiceRequest> preferredOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("preferredDate"), from);
    }

    /** preferredDate <= to (inclusive). Requests without a preferred date never match. */
    public static Specification<ServiceRequest> preferredOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("preferredDate"), to);
    }

    /** Case-insensitive "contains" on the vehicle's plate; the text is normalized like stored plates first. */
    public static Specification<ServiceRequest> licensePlateContains(String licensePlate) {
        String pattern = licensePlateContainsPattern(licensePlate);
        return (root, query, cb) -> cb.like(cb.lower(root.get("vehicle").get("licensePlate")), pattern, LIKE_ESCAPE);
    }

    /** Equals on the vehicle FK. Still combined with the ownership scope by the service. */
    public static Specification<ServiceRequest> hasVehicleId(Long vehicleId) {
        return (root, query, cb) -> cb.equal(root.get("vehicle").get("id"), vehicleId);
    }
}
