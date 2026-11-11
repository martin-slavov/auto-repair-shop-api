package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemCreateDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemResponseDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemUpdateDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.service.CatalogItemService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Catalog Items", description = "Catalog items management")
@RestController
@RequestMapping("/api/catalog-items")
public class CatalogItemController {

    private final CatalogItemService catalogItemService;

    public CatalogItemController(CatalogItemService catalogItemService) {
        this.catalogItemService = catalogItemService;
    }

    @Operation(summary = "Create a new catalog item")
    @PostMapping
    public ResponseEntity<CatalogItemResponseDTO> createCatalogItem(@Valid @RequestBody CatalogItemCreateDTO catalogItemRequest) {
        CatalogItemResponseDTO response = catalogItemService.createCatalogItem(catalogItemRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Search catalog items (paged). Guests and customers see active items only; " +
            "admins see all and can filter by 'active'.")
    @GetMapping
    public ResponseEntity<PageResponse<CatalogItemResponseDTO>> getAllCatalogItems(
            @Parameter(description = "Name contains (case-insensitive)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Minimum base price (inclusive)")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum base price (inclusive)")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "ADMIN only: filter by active flag. Ignored for other users.")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number, 0-based (default 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 10, max 50)")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "field,asc|desc - fields: name, price, estimatedHours, createdAt (default name,asc)")
            @RequestParam(required = false) String sort) {

        boolean isAdmin = SecurityUtils.currentUserHasRole("ADMIN");
        PageResponse<CatalogItemResponseDTO> response = catalogItemService.getAllCatalogItems(
                name, minPrice, maxPrice, active, isAdmin, page, size, sort);
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Update an existing catalog item")
    @PutMapping("/{id}")
    public ResponseEntity<CatalogItemResponseDTO> updateCatalogItem(@PathVariable Long id, @Valid @RequestBody CatalogItemUpdateDTO catalogItemRequest) {
        CatalogItemResponseDTO response = catalogItemService.updateCatalogItem(id, catalogItemRequest);
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Get a catalog item by ID (inactive items are visible to admins only)")
    @GetMapping("/{id}")
    public ResponseEntity<CatalogItemResponseDTO> getCatalogItemById(@PathVariable Long id) {
        boolean isAdmin = SecurityUtils.currentUserHasRole("ADMIN");
        CatalogItemResponseDTO catalogItem = catalogItemService.getCatalogItemById(id, isAdmin);
        return ResponseEntity.ok().body(catalogItem);
    }

    @Operation(summary = "Delete a catalog item by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCatalogItem(@PathVariable Long id) {
        catalogItemService.deleteCatalogItem(id);
        return ResponseEntity.noContent().build();
    }
}