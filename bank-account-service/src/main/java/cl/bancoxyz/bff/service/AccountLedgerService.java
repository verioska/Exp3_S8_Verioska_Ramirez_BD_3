package cl.bancoxyz.bff.service;

import cl.bancoxyz.bff.model.InterestAccount;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountLedgerService {

    private final LegacyDataService legacyDataService;
    private final Map<Long, BigDecimal> runtimeBalances = new ConcurrentHashMap<>();
    private final List<AtmWithdrawal> withdrawals = new ArrayList<>();

    public AccountLedgerService(LegacyDataService legacyDataService) {
        this.legacyDataService = legacyDataService;
    }

    @PostConstruct
    void initializeBalances() {
        for (InterestAccount account : legacyDataService.accounts()) {
            runtimeBalances.put(account.cuentaId(), account.saldo());
        }
    }

    public Optional<BigDecimal> balance(long accountId) {
        return Optional.ofNullable(runtimeBalances.get(accountId));
    }

    public synchronized WithdrawalResult withdraw(long accountId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return new WithdrawalResult(false, "El monto debe ser mayor a cero", balance(accountId).orElse(BigDecimal.ZERO));
        }
        BigDecimal currentBalance = runtimeBalances.get(accountId);
        if (currentBalance == null) {
            return new WithdrawalResult(false, "Cuenta no encontrada", BigDecimal.ZERO);
        }
        if (currentBalance.compareTo(amount) < 0) {
            return new WithdrawalResult(false, "Saldo insuficiente", currentBalance);
        }
        BigDecimal updatedBalance = currentBalance.subtract(amount);
        runtimeBalances.put(accountId, updatedBalance);
        withdrawals.add(new AtmWithdrawal(accountId, amount, updatedBalance, LocalDateTime.now()));
        return new WithdrawalResult(true, "Retiro aprobado", updatedBalance);
    }

    public List<AtmWithdrawal> withdrawals() {
        return List.copyOf(withdrawals);
    }

    public record WithdrawalResult(boolean aprobado, String mensaje, BigDecimal saldoDisponible) {
    }

    public record AtmWithdrawal(long cuentaId, BigDecimal monto, BigDecimal saldoFinal, LocalDateTime fechaHora) {
    }
}
