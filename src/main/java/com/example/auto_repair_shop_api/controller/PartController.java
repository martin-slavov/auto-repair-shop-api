package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.PartCreateDTO;
import com.example.auto_repair_shop_api.dto.PartResponseDTO;
import com.example.auto_repair_shop_api.service.PartService;
import com.example.auto_repair_shop_api.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @PostMapping
    public ResponseEntity<PartResponseDTO> createPart(@Valid @RequestBody PartCreateDTO partCreateDTO) {
        PartResponseDTO response = partService.createPart(partCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<?>> getParts() {

        String currentUsername = SecurityUtils.getCurrentUsername();

        List<?> result = partService.getPartsForCurrentUser(currentUsername);

        return ResponseEntity.ok().body(result);
    }
}
