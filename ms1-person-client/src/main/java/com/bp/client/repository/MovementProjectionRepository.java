package com.bp.client.repository;

import com.bp.client.models.MovementProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementProjectionRepository extends JpaRepository<MovementProjection, Long> {

    List<MovementProjection> findByClientIdOrderByDateDesc(String clientId);
}
