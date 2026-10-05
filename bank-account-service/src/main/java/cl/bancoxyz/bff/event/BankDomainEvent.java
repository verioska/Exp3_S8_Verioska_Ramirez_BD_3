package cl.bancoxyz.bff.event;

import java.time.Instant;

public record BankDomainEvent(
        String eventType,
        long cuentaId,
        String channel,
        String status,
        Instant occurredAt) {
}
