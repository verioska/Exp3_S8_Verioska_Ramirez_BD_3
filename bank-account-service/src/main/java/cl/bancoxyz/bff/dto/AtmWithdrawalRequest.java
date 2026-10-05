package cl.bancoxyz.bff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AtmWithdrawalRequest(
        @NotNull
        @DecimalMin(value = "1.00")
        BigDecimal monto
) {
}
