package cl.bancoxyz.bff.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRecord(
        long id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo
) {
}
