package cl.bancoxyz.bff.model;

public record DataQualityReport(
        String archivo,
        int registrosLeidos,
        int registrosValidos,
        int registrosOmitidos
) {
}
