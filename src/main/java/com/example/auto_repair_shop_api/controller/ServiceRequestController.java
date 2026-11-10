package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.servicerequest.ApproveRequestDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestCreateDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestResponseDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestUpdateDTO;
import com.example.auto_repair_shop_api.service.ServiceRequestService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Service Requests", description = "Submit and manage vehicle service requests")
@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @Operation(summary = "Submit a new service request for a customer's vehicle")
    @PostMapping
    public ResponseEntity<ServiceRequestResponseDTO> createServiceRequest(@Valid @RequestBody ServiceRequestCreateDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        ServiceRequestResponseDTO result = serviceRequestService.createRequest(dto, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Get service requests (own requests for customers, pending requests for admins)")
    @GetMapping
    public ResponseEntity<List<ServiceRequestResponseDTO>> getServiceRequestsForCurrentUser() {

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<ServiceRequestResponseDTO> result = serviceRequestService.getRequestsForCurrentUser(currentUsername);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Approve a pending service request and assign a mechanic (admin only)")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ServiceRequestResponseDTO> approveServiceRequest(@PathVariable Long id, @Valid @RequestBody ApproveRequestDTO dto) {

        ServiceRequestResponseDTO result = serviceRequestService.approveRequest(id, dto);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Reject a pending service request (admin only)")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ServiceRequestResponseDTO> rejectServiceRequest(@PathVariable Long id) {

        ServiceRequestResponseDTO result = serviceRequestService.rejectRequest(id);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Update a pending service request (owner or admin)")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceRequestResponseDTO> updateServiceRequest(@PathVariable Long id,
                                                                          @Valid @RequestBody ServiceRequestUpdateDTO dto) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        ServiceRequestResponseDTO result = serviceRequestService.updateRequest(id, dto, currentUsername);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Delete a pending service request (owner or admin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceRequest(@PathVariable Long id) {

        String currentUsername = SecurityUtils.getCurrentUsername();
        serviceRequestService.deleteRequest(id, currentUsername);
        return ResponseEntity.noContent().build();
    }
}
