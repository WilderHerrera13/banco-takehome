package com.bp.account.messaging;

import com.bp.account.models.ClientProjection;
import com.bp.account.repository.ClientProjectionRepository;
import com.bp.shared.event.ClientRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClientRegisteredConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ClientRegisteredConsumer.class);

    private final ClientProjectionRepository repository;

    public ClientRegisteredConsumer(ClientProjectionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.kafka.topic.client}")
    @Transactional
    public void onClienteRegistrado(ClientRegisteredEvent evento) {
        repository.save(new ClientProjection(evento.clientId(), evento.name(), evento.identification(),
                evento.fechaEvento()));
        LOG.info("Client {} projected locally", evento.clientId());
    }
}
