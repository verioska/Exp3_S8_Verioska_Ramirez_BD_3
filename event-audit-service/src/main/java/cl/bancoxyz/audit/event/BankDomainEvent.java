package cl.bancoxyz.audit.event;

import java.time.Instant;

public record BankDomainEvent(
        String eventType,
        long cuentaId,
        String channel,
        String status,
        Instant occurredAt) {
}
