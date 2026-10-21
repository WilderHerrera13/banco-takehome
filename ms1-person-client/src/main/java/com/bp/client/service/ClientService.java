package com.bp.client.service;

import com.bp.client.models.Client;
import com.bp.client.exception.ClientNotFoundException;
import com.bp.client.exception.DuplicateIdentificationException;
import com.bp.client.mappers.ClientMapper;
import com.bp.client.repository.ClientRepository;
import com.bp.shared.dto.ClientDto;
import com.bp.shared.event.ClientDeletedEvent;
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
    private final ClientDeletedEventPublisher deletedEventPublisher;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository clientRepository, ClientEventPublisher eventPublisher,
            ClientDeletedEventPublisher deletedEventPublisher, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
        this.deletedEventPublisher = deletedEventPublisher;
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
        Client client = clientMapper.toEntity(request);
        client.setClientId(clientId);
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
        clientMapper.update(client, request);
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
        deletedEventPublisher.publish(new ClientDeletedEvent(clientId, Instant.now()));
        LOG.info("Client {} deleted", clientId);
    }


}
