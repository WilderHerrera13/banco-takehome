package com.bp.account.messaging;

import com.bp.account.models.ClientProjection;
import com.bp.account.repository.ClientProjectionRepository;
import com.bp.shared.event.ClientDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClientDeletedConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ClientDeletedConsumer.class);

    private final ClientProjectionRepository repository;

    public ClientDeletedConsumer(ClientProjectionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.kafka.topic.client.deleted}")
    @Transactional
    public void onClienteEliminado(ClientDeletedEvent evento) {
        repository.findById(evento.clientId()).ifPresent(projection -> {
            repository.delete(projection);
            LOG.info("Client {} removed from local projection", evento.clientId());
        });
    }
}
