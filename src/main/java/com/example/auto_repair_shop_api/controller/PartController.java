package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.part.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.part.PartResponseDTO;
import com.example.auto_repair_shop_api.dto.part.PartUpdateDTO;
import com.example.auto_repair_shop_api.dto.part.PartView;
import com.example.auto_repair_shop_api.service.PartService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Parts", description = "Parts catalog management")
@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @Operation(summary = "Add a new part to the catalog (admin only)")
    @PostMapping
    public ResponseEntity<PartResponseDTO> createPart(@Valid @RequestBody PartCreateDTO partCreateDTO) {
        PartResponseDTO response = partService.createPart(partCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Search the parts catalog (paged). Admins get full details (PartResponseDTO) and see all parts; " +
            "mechanics get PartCatalogDTO (no unit cost) and see active parts only.")
    @GetMapping
    public ResponseEntity<PageResponse<PartView>> getParts(
            @Parameter(description = "Name contains (case-insensitive)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Serial number contains (case-insensitive)")
            @RequestParam(required = false) String serialNumber,
            @Parameter(description = "Minimum unit sell price (inclusive)")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum unit sell price (inclusive)")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "ADMIN only: filter by active flag. Ignored for other users.")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number, 0-based (default 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 10, max 50)")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "field,asc|desc - fields: name, serialNumber, unitSellPrice; " +
                    "admins also unitCost (default name,asc)")
            @RequestParam(required = false) String sort) {

        boolean isAdmin = SecurityUtils.currentUserHasRole("ADMIN");
        PageResponse<PartView> result = partService.getParts(
                name, serialNumber, minPrice, maxPrice, active, isAdmin, page, size, sort);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Update a part (admin only)")
    @PutMapping("/{id}")
    public ResponseEntity<PartResponseDTO> updatePart(@PathVariable Long id, @Valid @RequestBody PartUpdateDTO partUpdateDTO) {

        PartResponseDTO response = partService.updatePart(id, partUpdateDTO);
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Delete a part (admin only); fails if the part is used in a service visit")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {

        partService.deletePart(id);
        return ResponseEntity.noContent().build();
    }
}
