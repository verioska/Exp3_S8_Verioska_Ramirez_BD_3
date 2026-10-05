package cl.bancoxyz.bff.controller;

import cl.bancoxyz.bff.dto.MobileMovementResponse;
import cl.bancoxyz.bff.dto.MobileSummaryResponse;
import cl.bancoxyz.bff.model.AnnualMovement;
import cl.bancoxyz.bff.service.AccountLedgerService;
import cl.bancoxyz.bff.service.LegacyDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile")
public class MobileBffController {

    private final LegacyDataService legacyDataService;
    private final AccountLedgerService accountLedgerService;

    public MobileBffController(LegacyDataService legacyDataService, AccountLedgerService accountLedgerService) {
        this.legacyDataService = legacyDataService;
        this.accountLedgerService = accountLedgerService;
    }

    @GetMapping("/resumen/{cuentaId}")
    public ResponseEntity<MobileSummaryResponse> summary(@PathVariable long cuentaId) {
        return legacyDataService.findAccount(cuentaId)
                .map(account -> ResponseEntity.ok(new MobileSummaryResponse(
                        "mobile",
                        account.cuentaId(),
                        account.nombre(),
                        account.tipo(),
                        accountLedgerService.balance(cuentaId).orElse(account.saldo()),
                        lightweightMovements(cuentaId, 5))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/movimientos/{cuentaId}")
    public List<MobileMovementResponse> movements(@PathVariable long cuentaId, @RequestParam(defaultValue = "10") int limit) {
        return lightweightMovements(cuentaId, Math.min(limit, 30));
    }

    private List<MobileMovementResponse> lightweightMovements(long cuentaId, int limit) {
        return legacyDataService.movementsByAccount(cuentaId).stream()
                .limit(limit)
                .map(this::toMobileMovement)
                .toList();
    }

    private MobileMovementResponse toMobileMovement(AnnualMovement movement) {
        return new MobileMovementResponse(movement.fecha(), movement.transaccion(), movement.monto());
    }
}
