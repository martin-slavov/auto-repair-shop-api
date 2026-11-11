package com.example.auto_repair_shop_api.util;

import com.example.auto_repair_shop_api.exception.InvalidSortParameterException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Builds a PageRequest from raw query parameters for every list endpoint.
 * - page below 0 becomes 0
 * - size below 1 becomes DEFAULT_SIZE, size above MAX_SIZE is clamped to MAX_SIZE
 * - sort is "field" or "field,asc|desc", where field must be a key of allowedSortFields
 *   (API name -> entity property). Anything else throws InvalidSortParameterException (400).
 */
public final class PageRequestFactory {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private static final String ID_PROPERTY = "id";

    private PageRequestFactory() {
    }

    public static PageRequest create(Integer page, Integer size, String sort,
                                     Map<String, String> allowedSortFields, Sort defaultSort) {
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, parseSort(sort, allowedSortFields, defaultSort));
    }

    private static Sort parseSort(String sort, Map<String, String> allowedSortFields, Sort defaultSort) {
        Sort result;
        if (sort == null || sort.isBlank()) {
            result = defaultSort;
        } else {
            String[] parts = sort.split(",");
            if (parts.length > 2) {
                throw new InvalidSortParameterException(
                        "Sort must be in the format 'field' or 'field,asc|desc' (only one sort field is allowed)");
            }

            String field = parts[0].trim();
            String property = allowedSortFields.get(field);
            if (property == null) {
                throw new InvalidSortParameterException(
                        "Unknown sort field '" + field + "'. Allowed: " + allowedFieldsList(allowedSortFields));
            }

            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length == 2) {
                String rawDirection = parts[1].trim();
                direction = Sort.Direction.fromOptionalString(rawDirection)
                        .orElseThrow(() -> new InvalidSortParameterException(
                                "Unknown sort direction '" + rawDirection + "'. Allowed: asc, desc"));
            }
            result = Sort.by(direction, property);
        }

        // Tie-breaker on id so rows with equal sort values keep a stable order across pages.
        if (result.getOrderFor(ID_PROPERTY) == null) {
            result = result.and(Sort.by(Sort.Direction.ASC, ID_PROPERTY));
        }
        return result;
    }

    private static String allowedFieldsList(Map<String, String> allowedSortFields) {
        return String.join(", ", allowedSortFields.keySet().stream().sorted().toList());
    }
}
