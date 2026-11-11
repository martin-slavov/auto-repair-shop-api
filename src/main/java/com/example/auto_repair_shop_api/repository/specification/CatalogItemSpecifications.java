package com.example.auto_repair_shop_api.repository.specification;

import com.example.auto_repair_shop_api.model.CatalogItem;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.LIKE_ESCAPE;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.containsPattern;

/**
 * Reusable filter pieces for CatalogItem searches. The service only adds a piece
 * when its query parameter was actually sent.
 */
public final class CatalogItemSpecifications {

    private CatalogItemSpecifications() {
    }

    /** Case-insensitive "contains". '%' and '_' typed by the user are matched literally. */
    public static Specification<CatalogItem> nameContains(String name) {
        String pattern = containsPattern(name);
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE);
    }

    public static Specification<CatalogItem> priceAtLeast(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice);
    }

    public static Specification<CatalogItem> priceAtMost(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice);
    }

    public static Specification<CatalogItem> hasActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
