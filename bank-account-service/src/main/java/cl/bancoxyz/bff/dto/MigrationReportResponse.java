package cl.bancoxyz.bff.dto;

import cl.bancoxyz.bff.model.DataQualityReport;

import java.time.LocalDateTime;
import java.util.List;

public record MigrationReportResponse(
        String origen,
        long cuentaId,
        String estado,
        int cuentasMigradas,
        int transaccionesMigradas,
        int movimientosMigrados,
        List<DataQualityReport> calidadDatos,
        LocalDateTime generadoEn
) {
}
