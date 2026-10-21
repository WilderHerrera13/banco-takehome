package com.bp.client.service;

import com.bp.shared.event.ClientDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClientDeletedEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(ClientDeletedEventPublisher.class);

    private final KafkaTemplate<String, ClientDeletedEvent> kafkaTemplate;
    private final String topic;

    public ClientDeletedEventPublisher(KafkaTemplate<String, ClientDeletedEvent> kafkaTemplate,
            @Value("${app.kafka.topic.client.deleted}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(ClientDeletedEvent evento) {
        kafkaTemplate.send(topic, evento.clientId(), evento)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        LOG.error("Event {} could not be published for deleted client {}", topic,
                                evento.clientId(), error);
                    }
                });
        LOG.info("Event {} published for deleted client {}", topic, evento.clientId());
    }
}
