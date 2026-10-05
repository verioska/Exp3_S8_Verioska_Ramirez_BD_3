package cl.bancoxyz.bff.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnnualMovement(
        long cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion
) {
}
