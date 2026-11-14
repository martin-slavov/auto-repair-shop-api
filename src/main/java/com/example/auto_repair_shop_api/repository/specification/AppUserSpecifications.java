package com.example.auto_repair_shop_api.repository.specification;

import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.enums.Role;
import org.springframework.data.jpa.domain.Specification;

import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.LIKE_ESCAPE;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.containsPattern;

/**
 * Reusable filter pieces for the admin user search. The service only adds a piece
 * when its query parameter was sent.
 */
public final class AppUserSpecifications {

    private AppUserSpecifications() {
    }

    /** Case-insensitive "contains". '%' and '_' typed by the user are matched literally. */
    public static Specification<AppUser> usernameContains(String username) {
        String pattern = containsPattern(username);
        return (root, query, cb) -> cb.like(cb.lower(root.get("username")), pattern, LIKE_ESCAPE);
    }

    /** Case-insensitive "contains". '%' and '_' typed by the user are matched literally. */
    public static Specification<AppUser> emailContains(String email) {
        String pattern = containsPattern(email);
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE);
    }

    public static Specification<AppUser> hasRole(Role role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<AppUser> isEnabled(boolean enabled) {
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }
}
