package com.example.auto_repair_shop_api.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceVisitRepositoryTest {

    @Test
    void listQueryLoadsTheVehicleAndTheRequestWithTheVisits() throws NoSuchMethodException {
        EntityGraph graph = ServiceVisitRepository.class
                .getMethod("findAll", Specification.class, Pageable.class)
                .getAnnotation(EntityGraph.class);

        assertThat(graph).isNotNull();
        assertThat(graph.attributePaths()).containsExactlyInAnyOrder("vehicle", "serviceRequest");
    }
}
