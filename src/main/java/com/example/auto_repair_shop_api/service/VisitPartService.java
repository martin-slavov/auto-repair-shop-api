package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.visitpart.VisitPartCreateDTO;
import com.example.auto_repair_shop_api.dto.visitpart.VisitPartResponseDTO;
import com.example.auto_repair_shop_api.dto.visitpart.VisitPartUpdateDTO;

import java.util.List;

public interface VisitPartService {

    VisitPartResponseDTO addPartToVisit(Long visitId, VisitPartCreateDTO visitPartCreateDTO, String currentUsername);

    List<VisitPartResponseDTO> getPartsForVisit(Long visitId, String currentUsername);

    VisitPartResponseDTO updateVisitPart(Long visitId, Long partLineId, VisitPartUpdateDTO dto, String currentUsername);

    void deleteVisitPart(Long visitId, Long partLineId, String currentUsername);
}
