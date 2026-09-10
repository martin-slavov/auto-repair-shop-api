package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.VisitPartCreateDTO;
import com.example.auto_repair_shop_api.dto.VisitPartResponseDTO;

import java.util.List;

public interface VisitPartService {

    VisitPartResponseDTO addPartToVisit(Long visitId, VisitPartCreateDTO visitPartCreateDTO, String currentUsername);

    List<VisitPartResponseDTO> getPartsForVisit(Long visitId, String currentUsername);
}
