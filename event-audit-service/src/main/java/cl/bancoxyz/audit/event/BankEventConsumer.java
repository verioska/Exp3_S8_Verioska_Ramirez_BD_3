package cl.bancoxyz.audit.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class BankEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BankEventConsumer.class);

    @KafkaListener(topics = "${bank.events.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(BankDomainEvent event) {
        LOGGER.info(
                "Evento bancario auditado: tipo={}, cuentaId={}, canal={}, estado={}, fecha={}",
                event.eventType(),
                event.cuentaId(),
                event.channel(),
                event.status(),
                event.occurredAt());
    }
}
