package com.bp.client.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client")
public class Client extends Person {

    @Id
    @Column(name = "client_id", nullable = false, length = 40)
    private String clientId;

    @Column(name = "password", nullable = false, length = 120)
    private String password;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Client() {
    }

    public Client(String clientId, String name, Gender gender, Integer age, String identification,
            String address, String phone, String password, boolean active) {
        super(name, gender, age, identification, address, phone);
        this.clientId = clientId;
        this.password = password;
        this.active = active;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
