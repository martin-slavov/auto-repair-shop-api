package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitUpdateDTO;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ServiceVisitService {

    ServiceVisitResponseDTO createVisitFromRequest(ServiceRequest serviceRequest, Long mechanicId, LocalDateTime scheduledDate);

    List<ServiceVisitResponseDTO> getVisitsForMechanic(String currentUsername);

    ServiceVisitResponseDTO updateVisitStatus(Long visitId, VisitStatus newStatus, String currentUsername);

    ServiceVisitResponseDTO completeVisit(Long visitId, String technicalNotes, BigDecimal hoursWorked, String currentUsername);

    ServiceVisitResponseDTO updateVisit(Long visitId, ServiceVisitUpdateDTO dto);

    void deleteVisit(Long visitId);
}
