package com.example.auto_repair_shop_api.repository;

import com.example.auto_repair_shop_api.model.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long>, JpaSpecificationExecutor<CatalogItem> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<CatalogItem> findTop100ByActiveTrueOrderByNameAsc();
}
