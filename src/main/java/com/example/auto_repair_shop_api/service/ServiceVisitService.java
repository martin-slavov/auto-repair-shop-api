package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ServiceVisitService {

    ServiceVisitResponseDTO createVisitFromRequest(ServiceRequest serviceRequest, Long mechanicId, LocalDateTime scheduledDate);

    List<ServiceVisitResponseDTO> getVisitsForMechanic(String currentUsername);

    ServiceVisitResponseDTO updateVisitStatus(Long visitId, VisitStatus newStatus, String currentUsername);

    ServiceVisitResponseDTO completeVisit(Long visitId, String technicalNotes, String currentUsername);
}
