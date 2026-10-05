package cl.bancoxyz.bff.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@ConditionalOnProperty(name = "bank.events.kafka.enabled", havingValue = "true")
public class BankEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BankEventPublisher.class);

    private final KafkaTemplate<String, BankDomainEvent> kafkaTemplate;
    private final String topic;

    public BankEventPublisher(
            KafkaTemplate<String, BankDomainEvent> kafkaTemplate,
            @Value("${bank.events.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(String eventType, long cuentaId, String channel, String status) {
        BankDomainEvent event = new BankDomainEvent(eventType, cuentaId, channel, status, Instant.now());
        kafkaTemplate.send(topic, String.valueOf(cuentaId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        LOGGER.warn("No se pudo publicar evento {} para cuenta {}", eventType, cuentaId, ex);
                    }
                });
    }
}
