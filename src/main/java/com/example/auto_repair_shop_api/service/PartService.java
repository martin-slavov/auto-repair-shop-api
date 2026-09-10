package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.PartResponseDTO;

import java.util.List;

public interface PartService {

    PartResponseDTO createPart(PartCreateDTO dto);

    List<?> getPartsForCurrentUser(String currentUsername);
}
