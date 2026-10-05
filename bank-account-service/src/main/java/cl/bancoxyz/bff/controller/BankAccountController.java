package cl.bancoxyz.bff.controller;

import cl.bancoxyz.bff.dto.AccountSummaryResponse;
import cl.bancoxyz.bff.dto.MigrationReportResponse;
import cl.bancoxyz.bff.event.BankEventPublisher;
import cl.bancoxyz.bff.model.AnnualMovement;
import cl.bancoxyz.bff.model.DataQualityReport;
import cl.bancoxyz.bff.model.InterestAccount;
import cl.bancoxyz.bff.model.TransactionRecord;
import cl.bancoxyz.bff.service.AccountLedgerService;
import cl.bancoxyz.bff.service.LegacyAuditService;
import cl.bancoxyz.bff.service.LegacyDataService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BankAccountController {

    private final LegacyDataService legacyDataService;
    private final AccountLedgerService accountLedgerService;
    private final LegacyAuditService legacyAuditService;
    private final ObjectProvider<BankEventPublisher> eventPublisher;

    public BankAccountController(
            LegacyDataService legacyDataService,
            AccountLedgerService accountLedgerService,
            LegacyAuditService legacyAuditService,
            ObjectProvider<BankEventPublisher> eventPublisher) {
        this.legacyDataService = legacyDataService;
        this.accountLedgerService = accountLedgerService;
        this.legacyAuditService = legacyAuditService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/accounts")
    public List<InterestAccount> accounts() {
        return legacyDataService.accounts();
    }

    @GetMapping("/accounts/{cuentaId}")
    public ResponseEntity<AccountSummaryResponse> account(@PathVariable long cuentaId) {
        return legacyDataService.findAccount(cuentaId)
                .map(account -> ResponseEntity.ok(new AccountSummaryResponse(
                        account,
                        accountLedgerService.balance(cuentaId).orElse(account.saldo()),
                        legacyDataService.movementsByAccount(cuentaId).stream().limit(10).toList())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/accounts/{cuentaId}/movements")
    public List<AnnualMovement> movements(@PathVariable long cuentaId, @RequestParam(defaultValue = "20") int limit) {
        return legacyDataService.movementsByAccount(cuentaId).stream()
                .limit(Math.min(limit, 100))
                .toList();
    }

    @GetMapping("/accounts/{cuentaId}/migration-report")
    public ResponseEntity<MigrationReportResponse> migrationReport(@PathVariable long cuentaId) {
        return legacyDataService.findAccount(cuentaId)
                .map(account -> {
                    MigrationReportResponse response = legacyAuditService.reportForAccount(cuentaId);
                    eventPublisher.ifAvailable(publisher -> publisher.publish(
                            "MIGRATION_REPORT_GENERATED",
                            cuentaId,
                            "bank-account-service",
                            response.estado()));
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/migration/transactions")
    public List<TransactionRecord> transactions(@RequestParam(defaultValue = "50") int limit) {
        return legacyDataService.transactions().stream()
                .limit(Math.min(limit, 200))
                .toList();
    }

    @GetMapping("/migration/quality")
    public List<DataQualityReport> quality() {
        return legacyDataService.qualityReports();
    }
}
