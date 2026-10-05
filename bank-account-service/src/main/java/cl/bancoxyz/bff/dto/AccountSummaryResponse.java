package cl.bancoxyz.bff.dto;

import cl.bancoxyz.bff.model.AnnualMovement;
import cl.bancoxyz.bff.model.InterestAccount;

import java.math.BigDecimal;
import java.util.List;

public record AccountSummaryResponse(
        InterestAccount cuenta,
        BigDecimal saldoDisponible,
        List<AnnualMovement> ultimosMovimientos
) {
}
