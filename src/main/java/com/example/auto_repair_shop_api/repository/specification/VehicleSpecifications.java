package com.example.auto_repair_shop_api.repository.specification;

import com.example.auto_repair_shop_api.model.Vehicle;
import org.springframework.data.jpa.domain.Specification;

import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.LIKE_ESCAPE;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.containsPattern;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.licensePlateContainsPattern;

/**
 * Reusable filter pieces for Vehicle searches. The service always adds ownedBy(...) first
 * for non-admins and only adds the other pieces when their query parameter was sent.
 */
public final class VehicleSpecifications {

    private VehicleSpecifications() {
    }

    /** Ownership scope (and the admin-only ownerId filter). Compares the owner FK, no join needed. */
    public static Specification<Vehicle> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    /** Case-insensitive "contains" on the plate; the text is normalized like stored plates first. */
    public static Specification<Vehicle> licensePlateContains(String licensePlate) {
        String pattern = licensePlateContainsPattern(licensePlate);
        return (root, query, cb) -> cb.like(cb.lower(root.get("licensePlate")), pattern, LIKE_ESCAPE);
    }

    public static Specification<Vehicle> manufacturerContains(String manufacturer) {
        String pattern = containsPattern(manufacturer);
        return (root, query, cb) -> cb.like(cb.lower(root.get("manufacturer")), pattern, LIKE_ESCAPE);
    }

    public static Specification<Vehicle> modelContains(String model) {
        String pattern = containsPattern(model);
        return (root, query, cb) -> cb.like(cb.lower(root.get("model")), pattern, LIKE_ESCAPE);
    }

    public static Specification<Vehicle> yearAtLeast(Integer yearFrom) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("year"), yearFrom);
    }

    public static Specification<Vehicle> yearAtMost(Integer yearTo) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("year"), yearTo);
    }
}
