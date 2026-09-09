package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.CompleteVisitDTO;
import com.example.auto_repair_shop_api.dto.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.UpdateVisitStatusDTO;
import com.example.auto_repair_shop_api.service.ServiceVisitService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-visits")
public class ServiceVisitController {

    private final ServiceVisitService serviceVisitService;

    public ServiceVisitController(ServiceVisitService serviceVisitService) {
        this.serviceVisitService = serviceVisitService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceVisitResponseDTO>> getVisitsForMechanic() {

        String name = SecurityUtils.getCurrentUsername();
        List<ServiceVisitResponseDTO> result = serviceVisitService.getVisitsForMechanic(name);
        return ResponseEntity.ok().body(result);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceVisitResponseDTO> updateVisitStatus(@Valid @RequestBody UpdateVisitStatusDTO dto, @PathVariable Long id) {

        String name = SecurityUtils.getCurrentUsername();
        ServiceVisitResponseDTO result = serviceVisitService.updateVisitStatus(id, dto.newStatus(), name);
        return ResponseEntity.ok().body(result);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ServiceVisitResponseDTO> completeVisit(@PathVariable Long id, @Valid @RequestBody CompleteVisitDTO dto) {

        String name = SecurityUtils.getCurrentUsername();
        ServiceVisitResponseDTO result = serviceVisitService.completeVisit(id, dto.technicalNotes(), name);
        return ResponseEntity.ok().body(result);
    }
}
