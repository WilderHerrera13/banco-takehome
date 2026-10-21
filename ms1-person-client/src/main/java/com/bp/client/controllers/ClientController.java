package com.bp.client.controllers;

import com.bp.client.service.ClientService;
import com.bp.client.controllers.dto.ClientRequest;
import com.bp.shared.dto.ClientDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }


    @PostMapping
    public ResponseEntity<ClientDto> create(@Valid @RequestBody ClientRequest request) {
        ClientDto client = clientService.create(new ClientDto(null, request.name(), request.gender(),
                request.age(), request.identification(), request.address(), request.phone(),
                request.password(), request.active()));
        return ResponseEntity.status(HttpStatus.CREATED).body(client);
    }

    @GetMapping
    public List<ClientDto> list() {
        return clientService.list();
    }

    @GetMapping("/{clientId}")
    public ClientDto get(@PathVariable String clientId) {
        return clientService.get(clientId);
    }

    @PutMapping("/{clientId}")
    public ClientDto update(@PathVariable String clientId, @Valid @RequestBody ClientRequest request) {
        return clientService.update(clientId, new ClientDto(clientId, request.name(), request.gender(),
                request.age(), request.identification(), request.address(), request.phone(),
                request.password(), request.active()));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(@PathVariable String clientId) {
        clientService.delete(clientId);
        return ResponseEntity.noContent().build();
    }
}
