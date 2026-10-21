package com.bp.client.service;

import com.bp.client.models.Client;
import com.bp.client.models.Gender;
import com.bp.client.exception.ClientNotFoundException;
import com.bp.client.exception.DuplicateIdentificationException;
import com.bp.client.mappers.ClientMapper;
import com.bp.client.repository.ClientRepository;
import com.bp.shared.dto.ClientDto;
import com.bp.shared.event.ClientRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientService.class);

    private final ClientRepository clientRepository;
    private final ClientEventPublisher eventPublisher;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository clientRepository, ClientEventPublisher eventPublisher,
            ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
        this.clientMapper = clientMapper;
    }

    @Transactional
    public ClientDto create(ClientDto request) {
        if (clientRepository.existsByIdentification(request.identification())) {
            throw new DuplicateIdentificationException(request.identification());
        }
        String clientId = request.clientId() != null && !request.clientId().isBlank()
                ? request.clientId()
                : UUID.randomUUID().toString();
        Client client = new Client(clientId, request.name(), toGenero(request.gender()), request.age(),
                request.identification(), request.address(), request.phone(), request.password(),
                request.active() == null || request.active());
        Client guardado = clientRepository.save(client);
        eventPublisher.publish(new ClientRegisteredEvent(guardado.getClientId(), guardado.getName(),
                guardado.getIdentification(), Instant.now()));
        LOG.info("Client {} created", guardado.getClientId());
        return clientMapper.toDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<ClientDto> list() {
        return clientRepository.findAll().stream().map(clientMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ClientDto get(String clientId) {
        return clientMapper.toDto(clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId)));
    }

    @Transactional
    public ClientDto update(String clientId, ClientDto request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
        client.setName(request.name());
        client.setGender(toGenero(request.gender()));
        client.setAge(request.age());
        client.setAddress(request.address());
        client.setPhone(request.phone());
        client.setPassword(request.password());
        client.setActive(request.active() == null || request.active());
        Client guardado = clientRepository.save(client);
        eventPublisher.publish(new ClientRegisteredEvent(guardado.getClientId(), guardado.getName(),
                guardado.getIdentification(), Instant.now()));
        LOG.info("Client {} updated", clientId);
        return clientMapper.toDto(guardado);
    }

    @Transactional
    public void delete(String clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ClientNotFoundException(clientId);
        }
        clientRepository.deleteById(clientId);
        LOG.info("Client {} deleted", clientId);
    }

    private Gender toGenero(String amount) {
        return amount == null || amount.isBlank() ? null : Gender.valueOf(amount.toUpperCase());
    }

}
