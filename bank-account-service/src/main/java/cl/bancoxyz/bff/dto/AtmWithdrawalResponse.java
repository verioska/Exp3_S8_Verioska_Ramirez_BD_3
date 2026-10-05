package cl.bancoxyz.bff.dto;

import java.math.BigDecimal;

public record AtmWithdrawalResponse(
        String canal,
        long cuentaId,
        boolean aprobado,
        String mensaje,
        BigDecimal saldoDisponible
) {
}
