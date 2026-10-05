package cl.bancoxyz.bff.dto;

import java.math.BigDecimal;

public record AtmBalanceResponse(
        String canal,
        long cuentaId,
        BigDecimal saldoDisponible
) {
}
