package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.visitpart.VisitPartCreateDTO;
import com.example.auto_repair_shop_api.dto.visitpart.VisitPartResponseDTO;
import com.example.auto_repair_shop_api.dto.visitpart.VisitPartUpdateDTO;
import com.example.auto_repair_shop_api.service.VisitPartService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Visit Parts", description = "Track parts used during a service visit")
@RestController
@RequestMapping("/api/service-visits/{id}/parts")
public class VisitPartController {

    private final VisitPartService visitPartService;

    public VisitPartController(VisitPartService visitPartService) {
        this.visitPartService = visitPartService;
    }

    @Operation(summary = "Add a part to a visit (assigned mechanic only, visit must be in progress)")
    @PostMapping
    public ResponseEntity<VisitPartResponseDTO> addPartToVisit(@PathVariable Long id, @Valid @RequestBody VisitPartCreateDTO visitPartCreateDTO) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VisitPartResponseDTO result = visitPartService.addPartToVisit(id, visitPartCreateDTO, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Get all parts used in a visit")
    @GetMapping
    public ResponseEntity<List<VisitPartResponseDTO>> getPartsForVisit(@PathVariable Long id) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<VisitPartResponseDTO> result = visitPartService.getPartsForVisit(id, currentUsername);

        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Change the quantity of a part line (admin or assigned mechanic, visit not completed)")
    @PutMapping("/{partLineId}")
    public ResponseEntity<VisitPartResponseDTO> updateVisitPart(@PathVariable Long id, @PathVariable Long partLineId,
                                                                @Valid @RequestBody VisitPartUpdateDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VisitPartResponseDTO result = visitPartService.updateVisitPart(id, partLineId, dto, currentUsername);

        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Remove a part line from a visit (admin or assigned mechanic, visit not completed)")
    @DeleteMapping("/{partLineId}")
    public ResponseEntity<Void> deleteVisitPart(@PathVariable Long id, @PathVariable Long partLineId) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        visitPartService.deleteVisitPart(id, partLineId, currentUsername);

        return ResponseEntity.noContent().build();
    }
}
