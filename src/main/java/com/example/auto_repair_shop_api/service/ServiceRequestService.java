package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.servicerequest.ApproveRequestDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestCreateDTO;
import com.example.auto_repair_shop_api.dto.servicerequest.ServiceRequestResponseDTO;

import java.util.List;

public interface ServiceRequestService {

    ServiceRequestResponseDTO createRequest(ServiceRequestCreateDTO dto, String currentUsername);

    List<ServiceRequestResponseDTO> getRequestsForCurrentUser(String currentUsername);

    ServiceRequestResponseDTO approveRequest(Long requestId, ApproveRequestDTO dto);

    ServiceRequestResponseDTO rejectRequest(Long requestId);
}
