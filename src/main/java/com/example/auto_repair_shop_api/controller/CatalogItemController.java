package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemCreateDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemResponseDTO;
import com.example.auto_repair_shop_api.dto.catalogitem.CatalogItemUpdateDTO;
import com.example.auto_repair_shop_api.service.CatalogItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "Get all catalog items")
    @GetMapping
    public ResponseEntity<List<CatalogItemResponseDTO>> getAllCatalogItems() {
        List<CatalogItemResponseDTO> response = catalogItemService.getAllCatalogItems();
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Update an existing catalog item")
    @PutMapping("/{id}")
    public ResponseEntity<CatalogItemResponseDTO> updateCatalogItem(@PathVariable Long id, @Valid @RequestBody CatalogItemUpdateDTO catalogItemRequest) {
        CatalogItemResponseDTO response = catalogItemService.updateCatalogItem(id, catalogItemRequest);
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Get a catalog item by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CatalogItemResponseDTO> getCatalogItemById(@PathVariable Long id) {
        CatalogItemResponseDTO catalogItem = catalogItemService.getCatalogItemById(id);
        return ResponseEntity.ok().body(catalogItem);
    }

    @Operation(summary = "Delete a catalog item by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCatalogItem(@PathVariable Long id) {
        catalogItemService.deleteCatalogItem(id);
        return ResponseEntity.noContent().build();
    }
}