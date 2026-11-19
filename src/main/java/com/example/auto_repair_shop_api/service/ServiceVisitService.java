package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.servicevisit.AssistantHoursDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.AvailableSlotDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitResponseDTO;
import com.example.auto_repair_shop_api.dto.servicevisit.ServiceVisitUpdateDTO;
import com.example.auto_repair_shop_api.model.ServiceRequest;
import com.example.auto_repair_shop_api.model.enums.VisitStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ServiceVisitService {

    ServiceVisitResponseDTO createVisitFromRequest(ServiceRequest serviceRequest, Long mechanicId, LocalDateTime scheduledDate);

    PageResponse<ServiceVisitResponseDTO> getVisitsForCurrentUser(String currentUsername,
                                                                  VisitStatus status,
                                                                  LocalDate scheduledFrom, LocalDate scheduledTo,
                                                                  String licensePlate, Long vehicleId, Long mechanicId,
                                                                  Integer page, Integer size, String sort);

    ServiceVisitResponseDTO updateVisitStatus(Long visitId, VisitStatus newStatus, String currentUsername);

    ServiceVisitResponseDTO completeVisit(Long visitId, String technicalNotes, BigDecimal hoursWorked,
                                          List<AssistantHoursDTO> assistantHours, String currentUsername);

    ServiceVisitResponseDTO addAssistant(Long visitId, Long mechanicId);

    ServiceVisitResponseDTO removeAssistant(Long visitId, Long mechanicId);

    ServiceVisitResponseDTO updateVisit(Long visitId, ServiceVisitUpdateDTO dto);

    void deleteVisit(Long visitId);

    List<AvailableSlotDTO> getAvailableSlots(Long mechanicId, LocalDate date, Long excludeVisitId);
}
