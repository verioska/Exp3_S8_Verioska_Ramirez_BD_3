package cl.bancoxyz.bff.controller;

import cl.bancoxyz.bff.dto.WebDashboardResponse;
import cl.bancoxyz.bff.model.InterestAccount;
import cl.bancoxyz.bff.service.AccountLedgerService;
import cl.bancoxyz.bff.service.LegacyDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web")
public class WebBffController {

    private final LegacyDataService legacyDataService;
    private final AccountLedgerService accountLedgerService;

    public WebBffController(LegacyDataService legacyDataService, AccountLedgerService accountLedgerService) {
        this.legacyDataService = legacyDataService;
        this.accountLedgerService = accountLedgerService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<WebDashboardResponse> dashboard(@RequestParam(defaultValue = "103") long cuentaId) {
        return legacyDataService.findAccount(cuentaId)
                .map(account -> ResponseEntity.ok(new WebDashboardResponse(
                        "web",
                        account,
                        accountLedgerService.balance(cuentaId).orElse(account.saldo()),
                        legacyDataService.transactionSummaryByType(),
                        legacyDataService.movementsByAccount(cuentaId).stream().limit(20).toList(),
                        legacyDataService.qualityReports())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cuentas/{cuentaId}/detalle")
    public ResponseEntity<InterestAccount> accountDetail(@PathVariable long cuentaId) {
        return legacyDataService.findAccount(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
