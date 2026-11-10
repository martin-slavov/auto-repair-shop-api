package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.part.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.part.PartResponseDTO;
import com.example.auto_repair_shop_api.dto.part.PartUpdateDTO;

import java.util.List;

public interface PartService {

    PartResponseDTO createPart(PartCreateDTO dto);

    List<?> getPartsForCurrentUser(String currentUsername);

    PartResponseDTO updatePart(Long id, PartUpdateDTO dto);

    void deletePart(Long id);
}
