package com.example.auto_repair_shop_api.repository.specification;

import com.example.auto_repair_shop_api.model.Invoice;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.LIKE_ESCAPE;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.containsPattern;
import static com.example.auto_repair_shop_api.repository.specification.SpecificationUtils.licensePlateContainsPattern;

/**
 * Reusable filter pieces for Invoice searches. The service always adds vehicleOwnedBy(...) first
 * for every non-admin and only adds the other pieces when their query parameter was sent.
 */
public final class InvoiceSpecifications {

    private InvoiceSpecifications() {
    }

    /** Ownership scope: the invoiced visit's vehicle belongs to this user. Compares the owner FK on the vehicle row. */
    public static Specification<Invoice> vehicleOwnedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("visit").get("vehicle").get("owner").get("id"), ownerId);
    }

    public static Specification<Invoice> isPaid(boolean paid) {
        return (root, query, cb) -> cb.equal(root.get("paid"), paid);
    }

    /** Case-insensitive "contains". '%' and '_' typed by the user are matched literally. */
    public static Specification<Invoice> invoiceNumberContains(String invoiceNumber) {
        String pattern = containsPattern(invoiceNumber);
        return (root, query, cb) -> cb.like(cb.lower(root.get("invoiceNumber")), pattern, LIKE_ESCAPE);
    }

    /** Case-insensitive "contains" on the vehicle's plate; the text is normalized like stored plates first. */
    public static Specification<Invoice> licensePlateContains(String licensePlate) {
        String pattern = licensePlateContainsPattern(licensePlate);
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("visit").get("vehicle").get("licensePlate")), pattern, LIKE_ESCAPE);
    }

    /** Date part of issuedDate >= from (inclusive): issuedDate >= from 00:00. */
    public static Specification<Invoice> issuedOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issuedDate"), from.atStartOfDay());
    }

    /** Date part of issuedDate <= to (inclusive): issuedDate < (to + 1 day) 00:00. */
    public static Specification<Invoice> issuedOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThan(root.get("issuedDate"), to.plusDays(1).atStartOfDay());
    }

    public static Specification<Invoice> totalAtLeast(BigDecimal minTotal) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("totalPrice"), minTotal);
    }

    public static Specification<Invoice> totalAtMost(BigDecimal maxTotal) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("totalPrice"), maxTotal);
    }
}
