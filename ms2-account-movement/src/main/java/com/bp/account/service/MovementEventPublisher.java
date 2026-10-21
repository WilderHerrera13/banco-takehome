package com.bp.account.service;

import com.bp.shared.event.MovementRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MovementEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MovementEventPublisher.class);

    private final KafkaTemplate<String, MovementRegisteredEvent> kafkaTemplate;
    private final String topic;

    public MovementEventPublisher(KafkaTemplate<String, MovementRegisteredEvent> kafkaTemplate,
            @Value("${app.kafka.topic.movement}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(MovementRegisteredEvent evento) {
        kafkaTemplate.send(topic, evento.accountNumber(), evento);
        LOG.info("Event {} published for account {}", topic, evento.accountNumber());
    }
}
