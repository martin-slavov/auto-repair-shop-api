package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.servicerequest.ApproveRequestDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestCreateDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestResponseDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestUpdateDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.model.enums.RequestStatus;
import com.example.auto_repair_shop_api.model.enums.RequestType;

import java.time.LocalDate;

public interface ServiceRequestService {

    ServiceRequestResponseDTO createRequest(ServiceRequestCreateDTO dto, String currentUsername);

    PageResponse<ServiceRequestResponseDTO> getRequestsForCurrentUser(String currentUsername,
                                                                      RequestStatus status, RequestType type,
                                                                      LocalDate preferredFrom, LocalDate preferredTo,
                                                                      String licensePlate, Long vehicleId,
                                                                      Integer page, Integer size, String sort);

    ServiceRequestResponseDTO approveRequest(Long requestId, ApproveRequestDTO dto);

    ServiceRequestResponseDTO rejectRequest(Long requestId);

    ServiceRequestResponseDTO updateRequest(Long requestId, ServiceRequestUpdateDTO dto, String currentUsername);

    void deleteRequest(Long requestId, String currentUsername);
}
