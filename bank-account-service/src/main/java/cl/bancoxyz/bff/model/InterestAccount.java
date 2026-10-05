package cl.bancoxyz.bff.model;

import java.math.BigDecimal;

public record InterestAccount(
        long cuentaId,
        String nombre,
        BigDecimal saldo,
        int edad,
        String tipo
) {
}
