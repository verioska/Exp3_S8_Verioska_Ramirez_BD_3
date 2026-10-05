package cl.bancoxyz.bff.dto;

import cl.bancoxyz.bff.model.AnnualMovement;
import cl.bancoxyz.bff.model.DataQualityReport;
import cl.bancoxyz.bff.model.InterestAccount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record WebDashboardResponse(
        String canal,
        InterestAccount cuenta,
        BigDecimal saldoDisponible,
        Map<String, Long> resumenTransacciones,
        List<AnnualMovement> ultimosMovimientos,
        List<DataQualityReport> calidadDatos
) {
}
