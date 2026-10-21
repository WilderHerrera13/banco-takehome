package com.bp.account.repository;

import com.bp.account.models.ClientProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientProjectionRepository extends JpaRepository<ClientProjection, String> {

    Optional<ClientProjection> findByClientId(String clientId);
}
