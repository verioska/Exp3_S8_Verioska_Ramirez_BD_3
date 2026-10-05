package cl.bancoxyz.bff.dto;

import java.math.BigDecimal;
import java.util.List;

public record MobileSummaryResponse(
        String canal,
        long cuentaId,
        String nombre,
        String producto,
        BigDecimal saldoDisponible,
        List<MobileMovementResponse> ultimosMovimientos
) {
}
