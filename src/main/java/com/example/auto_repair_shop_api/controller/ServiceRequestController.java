package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.ServiceRequestCreateDTO;
import com.example.auto_repair_shop_api.dto.ServiceRequestResponseDTO;
import com.example.auto_repair_shop_api.service.ServiceRequestService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponseDTO> createServiceRequest(@Valid @RequestBody ServiceRequestCreateDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        ServiceRequestResponseDTO result = serviceRequestService.createRequest(dto, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<List<ServiceRequestResponseDTO>> getServiceRequestsForCurrentUser() {

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<ServiceRequestResponseDTO> result = serviceRequestService.getRequestsForCurrentUser(currentUsername);
        return ResponseEntity.ok().body(result);
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ServiceRequestResponseDTO> approveServiceRequest(@PathVariable Long id) {

        ServiceRequestResponseDTO result = serviceRequestService.approveRequest(id);
        return ResponseEntity.ok().body(result);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ServiceRequestResponseDTO> rejectServiceRequest(@PathVariable Long id) {

        ServiceRequestResponseDTO result = serviceRequestService.rejectRequest(id);
        return ResponseEntity.ok().body(result);
    }
}
