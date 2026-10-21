package com.bp.client.service;

import com.bp.shared.event.ClientRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClientEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(ClientEventPublisher.class);

    private final KafkaTemplate<String, ClientRegisteredEvent> kafkaTemplate;
    private final String topic;

    public ClientEventPublisher(KafkaTemplate<String, ClientRegisteredEvent> kafkaTemplate,
            @Value("${app.kafka.topic.client}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(ClientRegisteredEvent evento) {
        kafkaTemplate.send(topic, evento.clientId(), evento)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        LOG.error("Event {} could not be published for client {}", topic, evento.clientId(), error);
                    }
                });
        LOG.info("Event {} published for client {}", topic, evento.clientId());
    }
}
