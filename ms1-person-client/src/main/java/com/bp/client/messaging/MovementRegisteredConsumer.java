package com.bp.client.messaging;

import com.bp.client.models.MovementProjection;
import com.bp.client.repository.MovementProjectionRepository;
import com.bp.shared.event.MovementRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MovementRegisteredConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MovementRegisteredConsumer.class);

    private final MovementProjectionRepository repository;

    public MovementRegisteredConsumer(MovementProjectionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.kafka.topic.movement}")
    @Transactional
    public void onMovimientoRegistrado(MovementRegisteredEvent evento) {
        repository.save(new MovementProjection(evento.movementId(), evento.clientId(), evento.accountNumber(),
                evento.amount(), evento.balance(), evento.date()));
        LOG.info("Movement {} projected for client {}", evento.movementId(), evento.clientId());
    }
}
