package cl.bancoxyz.bff.service;

import cl.bancoxyz.bff.dto.MigrationReportResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LegacyAuditService {

    private final LegacyDataService legacyDataService;
    private final boolean forceLegacyAuditFailure;

    public LegacyAuditService(
            LegacyDataService legacyDataService,
            @Value("${bank.resilience.force-legacy-audit-failure:false}") boolean forceLegacyAuditFailure) {
        this.legacyDataService = legacyDataService;
        this.forceLegacyAuditFailure = forceLegacyAuditFailure;
    }

    @CircuitBreaker(name = "legacyAudit", fallbackMethod = "fallbackReportForAccount")
    public MigrationReportResponse reportForAccount(long accountId) {
        if (forceLegacyAuditFailure) {
            throw new IllegalStateException("Legacy audit endpoint unavailable");
        }
        return buildReport("legacy-audit-service", accountId, "VALIDATED");
    }

    public MigrationReportResponse fallbackReportForAccount(long accountId, Throwable ex) {
        return buildReport("fallback-local-cache", accountId, "VALIDATED_WITH_FALLBACK");
    }

    private MigrationReportResponse buildReport(String source, long accountId, String status) {
        return new MigrationReportResponse(
                source,
                accountId,
                status,
                legacyDataService.accounts().size(),
                legacyDataService.transactions().size(),
                legacyDataService.annualMovements().size(),
                legacyDataService.qualityReports(),
                LocalDateTime.now());
    }
}
