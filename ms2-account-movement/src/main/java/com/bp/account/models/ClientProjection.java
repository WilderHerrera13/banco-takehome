package com.bp.account.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "client_projection")
public class ClientProjection {

    @Id
    @Column(name = "client_id", nullable = false, length = 40)
    private String clientId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "identification", length = 40)
    private String identification;

    @Column(name = "event_timestamp")
    private Instant fechaEvento;

    protected ClientProjection() {
    }

    public ClientProjection(String clientId, String name, String identification, Instant fechaEvento) {
        this.clientId = clientId;
        this.name = name;
        this.identification = identification;
        this.fechaEvento = fechaEvento;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getIdentification() {
        return identification;
    }

    public Instant getEventDate() {
        return fechaEvento;
    }
}
