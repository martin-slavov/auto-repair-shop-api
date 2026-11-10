package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.part.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.part.PartResponseDTO;
import com.example.auto_repair_shop_api.dto.part.PartUpdateDTO;
import com.example.auto_repair_shop_api.service.PartService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "Get the parts catalog (full details for admin, prices only for mechanics)")
    @GetMapping
    public ResponseEntity<List<?>> getParts() {

        String currentUsername = SecurityUtils.getCurrentUsername();

        List<?> result = partService.getPartsForCurrentUser(currentUsername);

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
