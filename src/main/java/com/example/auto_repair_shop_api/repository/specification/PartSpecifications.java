package com.example.auto_repair_shop_api.repository.specification;

import com.example.auto_repair_shop_api.model.Part;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.LIKE_ESCAPE;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.containsPattern;

/**
 * Reusable filter pieces for Part searches. The service always adds hasActive(true) first for
 * non-admins and only adds the other pieces when their query parameter was sent.
 * There is deliberately no filter on unitCost (admin-only data).
 */
public final class PartSpecifications {

    private PartSpecifications() {
    }

    /** Case-insensitive "contains". '%' and '_' typed by the user are matched literally. */
    public static Specification<Part> nameContains(String name) {
        String pattern = containsPattern(name);
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE);
    }

    /** Case-insensitive "contains" on the serial number. */
    public static Specification<Part> serialNumberContains(String serialNumber) {
        String pattern = containsPattern(serialNumber);
        return (root, query, cb) -> cb.like(cb.lower(root.get("serialNumber")), pattern, LIKE_ESCAPE);
    }

    public static Specification<Part> sellPriceAtLeast(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("unitSellPrice"), minPrice);
    }

    public static Specification<Part> sellPriceAtMost(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("unitSellPrice"), maxPrice);
    }

    public static Specification<Part> hasActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
