package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.VisitPartCreateDTO;
import com.example.auto_repair_shop_api.dto.VisitPartResponseDTO;
import com.example.auto_repair_shop_api.service.VisitPartService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-visits/{id}/parts")
public class VisitPartController {

    private final VisitPartService visitPartService;

    public VisitPartController(VisitPartService visitPartService) {
        this.visitPartService = visitPartService;
    }

    @PostMapping
    public ResponseEntity<VisitPartResponseDTO> addPartToVisit(@PathVariable Long id, @Valid @RequestBody VisitPartCreateDTO visitPartCreateDTO) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        VisitPartResponseDTO result = visitPartService.addPartToVisit(id, visitPartCreateDTO, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<List<VisitPartResponseDTO>> getPartsForVisit(@PathVariable Long id) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<VisitPartResponseDTO> result = visitPartService.getPartsForVisit(id, currentUsername);

        return ResponseEntity.ok().body(result);
    }
}
