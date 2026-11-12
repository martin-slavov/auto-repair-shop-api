package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.dto.part.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.part.PartResponseDTO;
import com.example.auto_repair_shop_api.dto.part.PartUpdateDTO;
import com.example.auto_repair_shop_api.dto.part.PartView;

import java.math.BigDecimal;

public interface PartService {

    PartResponseDTO createPart(PartCreateDTO dto);

    PageResponse<PartView> getParts(String name, String serialNumber, BigDecimal minPrice, BigDecimal maxPrice,
                                    Boolean active, boolean isAdmin,
                                    Integer page, Integer size, String sort);

    PartResponseDTO updatePart(Long id, PartUpdateDTO dto);

    void deletePart(Long id);
}
