package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.ServiceRequestCreateDTO;
import com.example.auto_repair_shop_api.dto.ServiceRequestResponseDTO;

import java.util.List;

public interface ServiceRequestService {

    ServiceRequestResponseDTO createRequest(ServiceRequestCreateDTO dto, String currentUsername);

    List<ServiceRequestResponseDTO> getRequestsForCurrentUser(String currentUsername);

    ServiceRequestResponseDTO approveRequest(Long requestId);

    ServiceRequestResponseDTO rejectRequest(Long requestId);
}
