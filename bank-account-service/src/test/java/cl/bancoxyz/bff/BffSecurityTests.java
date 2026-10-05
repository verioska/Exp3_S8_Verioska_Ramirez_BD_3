package cl.bancoxyz.bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "server.ssl.enabled=false",
        "bank.resilience.force-legacy-audit-failure=true"
})
@AutoConfigureMockMvc
class BffSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiUserCanAccessMigratedAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts/106").with(jwt().authorities(role("BANK_API"))))
                .andExpect(status().isOk());
    }

    @Test
    void apiUserCannotAccessAtmOperations() throws Exception {
        mockMvc.perform(get("/api/atm/cuentas/106/saldo").with(jwt().authorities(role("BANK_API"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void atmUserCanAccessAtmBff() throws Exception {
        mockMvc.perform(get("/api/atm/cuentas/106/saldo").with(jwt().authorities(role("ATM"))))
                .andExpect(status().isOk());
    }

    @Test
    void migrationReportUsesFallbackWhenLegacyAuditFails() throws Exception {
        mockMvc.perform(get("/api/accounts/106/migration-report").with(jwt().authorities(role("BANK_API"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("fallback-local-cache"))
                .andExpect(jsonPath("$.estado").value("VALIDATED_WITH_FALLBACK"));
    }

    private static SimpleGrantedAuthority role(String role) {
        return new SimpleGrantedAuthority("ROLE_" + role);
    }
}
