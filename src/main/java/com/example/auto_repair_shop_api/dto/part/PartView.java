package com.example.auto_repair_shop_api.dto.part;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A part as returned by GET /api/parts: the full PartResponseDTO for admins,
 * or the PartCatalogDTO (without unitCost) for mechanics.
 */
@Schema(oneOf = {PartResponseDTO.class, PartCatalogDTO.class})
public sealed interface PartView permits PartResponseDTO, PartCatalogDTO {
}
