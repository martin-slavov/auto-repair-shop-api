package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.visitpart.VisitPartCreateDTO;
import com.example.auto_repair_shop_api.dto.visitpart.VisitPartResponseDTO;
import com.example.auto_repair_shop_api.dto.visitpart.VisitPartUpdateDTO;
import com.example.auto_repair_shop_api.service.VisitPartService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Add a part to a visit",
            description = "Access: MECHANIC. Only a mechanic assigned to the visit, while the visit is IN_PROGRESS; " +
                    "the part must be active.")
    @ApiResponse(responseCode = "201", description = "Part line added",
            content = @Content(schema = @Schema(implementation = VisitPartResponseDTO.class)))
    @PostMapping
    public ResponseEntity<VisitPartResponseDTO> addPartToVisit(@PathVariable Long id, @Valid @RequestBody VisitPartCreateDTO visitPartCreateDTO) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VisitPartResponseDTO result = visitPartService.addPartToVisit(id, visitPartCreateDTO, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "List the parts used in a visit",
            description = "Access: ADMIN, MECHANIC. Mechanics must be assigned to the visit.")
    @GetMapping
    public ResponseEntity<List<VisitPartResponseDTO>> getPartsForVisit(@PathVariable Long id) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<VisitPartResponseDTO> result = visitPartService.getPartsForVisit(id, currentUsername);

        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Change the quantity of a part line",
            description = "Access: ADMIN, MECHANIC. Mechanics must be assigned to the visit; " +
                    "not allowed once the visit is COMPLETED or INVOICED.")
    @PutMapping("/{partLineId}")
    public ResponseEntity<VisitPartResponseDTO> updateVisitPart(@PathVariable Long id, @PathVariable Long partLineId,
                                                                @Valid @RequestBody VisitPartUpdateDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VisitPartResponseDTO result = visitPartService.updateVisitPart(id, partLineId, dto, currentUsername);

        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Remove a part line from a visit",
            description = "Access: ADMIN, MECHANIC. Mechanics must be assigned to the visit; " +
                    "not allowed once the visit is COMPLETED or INVOICED.")
    @ApiResponse(responseCode = "204", description = "Part line removed")
    @DeleteMapping("/{partLineId}")
    public ResponseEntity<Void> deleteVisitPart(@PathVariable Long id, @PathVariable Long partLineId) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        visitPartService.deleteVisitPart(id, partLineId, currentUsername);

        return ResponseEntity.noContent().build();
    }
}
