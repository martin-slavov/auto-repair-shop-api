package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.part.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.part.PartResponseDTO;

import java.util.List;

public interface PartService {

    PartResponseDTO createPart(PartCreateDTO dto);

    List<?> getPartsForCurrentUser(String currentUsername);
}
