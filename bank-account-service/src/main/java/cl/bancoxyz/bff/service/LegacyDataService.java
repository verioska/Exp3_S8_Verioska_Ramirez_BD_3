package cl.bancoxyz.bff.service;

import cl.bancoxyz.bff.model.AnnualMovement;
import cl.bancoxyz.bff.model.DataQualityReport;
import cl.bancoxyz.bff.model.InterestAccount;
import cl.bancoxyz.bff.model.TransactionRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LegacyDataService {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-uuuu"),
            DateTimeFormatter.ofPattern("dd/MM/uuuu"),
            DateTimeFormatter.ofPattern("uuuu/MM/dd")
    );
    private static final Set<String> TRANSACTION_TYPES = Set.of("credito", "debito");
    private static final Set<String> ACCOUNT_TYPES = Set.of("ahorro", "prestamo", "hipoteca");
    private static final Set<String> MOVEMENT_TYPES = Set.of("deposito", "retiro", "compra", "pago");

    private final List<TransactionRecord> transactions = new ArrayList<>();
    private final List<InterestAccount> accounts = new ArrayList<>();
    private final List<AnnualMovement> annualMovements = new ArrayList<>();
    private final List<DataQualityReport> qualityReports = new ArrayList<>();

    @PostConstruct
    void load() throws IOException {
        loadTransactions();
        loadAccounts();
        loadAnnualMovements();
    }

    public List<TransactionRecord> transactions() {
        return transactions;
    }

    public List<InterestAccount> accounts() {
        return accounts;
    }

    public List<AnnualMovement> annualMovements() {
        return annualMovements;
    }

    public List<DataQualityReport> qualityReports() {
        return qualityReports;
    }

    public Optional<InterestAccount> findAccount(long accountId) {
        return accounts.stream()
                .filter(account -> account.cuentaId() == accountId)
                .findFirst();
    }

    public List<AnnualMovement> movementsByAccount(long accountId) {
        return annualMovements.stream()
                .filter(movement -> movement.cuentaId() == accountId)
                .sorted(Comparator.comparing(AnnualMovement::fecha).reversed())
                .toList();
    }

    public Map<String, Long> transactionSummaryByType() {
        return transactions.stream()
                .collect(Collectors.groupingBy(TransactionRecord::tipo, LinkedHashMap::new, Collectors.counting()));
    }

    private void loadTransactions() throws IOException {
        LoadedRows<TransactionRecord> loaded = readCsv("data/semana_3/transacciones.csv", columns -> {
            long id = parseLong(columns[0]).orElseThrow();
            LocalDate date = parseDate(columns[1]).orElseThrow();
            BigDecimal amount = parseMoney(columns[2]).filter(value -> value.signum() > 0).orElseThrow();
            String type = normalize(columns[3]);
            if (!TRANSACTION_TYPES.contains(type)) {
                throw new IllegalArgumentException("Tipo de transaccion invalido");
            }
            return new TransactionRecord(id, date, amount, type);
        });
        transactions.addAll(deduplicate(loaded.validRows(), TransactionRecord::id).values());
        qualityReports.add(new DataQualityReport("transacciones.csv", loaded.readRows(), transactions.size(), loaded.readRows() - transactions.size()));
    }

    private void loadAccounts() throws IOException {
        LoadedRows<InterestAccount> loaded = readCsv("data/semana_3/intereses.csv", columns -> {
            long accountId = parseLong(columns[0]).orElseThrow();
            String name = blankToDefault(columns[1], "Cliente Banco XYZ");
            BigDecimal balance = parseMoney(columns[2]).filter(value -> value.signum() >= 0).orElseThrow();
            int age = parseInt(columns[3]).filter(value -> value >= 18 && value <= 100).orElseThrow();
            String type = normalize(columns[4]);
            if (!ACCOUNT_TYPES.contains(type)) {
                throw new IllegalArgumentException("Tipo de cuenta invalido");
            }
            return new InterestAccount(accountId, name, balance, age, type);
        });
        accounts.addAll(deduplicate(loaded.validRows(), InterestAccount::cuentaId).values());
        qualityReports.add(new DataQualityReport("intereses.csv", loaded.readRows(), accounts.size(), loaded.readRows() - accounts.size()));
    }

    private void loadAnnualMovements() throws IOException {
        LoadedRows<AnnualMovement> loaded = readCsv("data/semana_3/cuentas_anuales.csv", columns -> {
            long accountId = parseLong(columns[0]).orElseThrow();
            LocalDate date = parseDate(columns[1]).orElseThrow();
            String type = normalize(columns[2]).replace("depósito", "deposito");
            if (!MOVEMENT_TYPES.contains(type)) {
                throw new IllegalArgumentException("Tipo de movimiento invalido");
            }
            BigDecimal amount = parseMoney(columns[3]).filter(value -> value.signum() > 0).orElseThrow();
            String description = blankToDefault(columns[4], "Movimiento sin descripcion");
            return new AnnualMovement(accountId, date, type, amount, description);
        });
        annualMovements.addAll(loaded.validRows());
        qualityReports.add(new DataQualityReport("cuentas_anuales.csv", loaded.readRows(), annualMovements.size(), loaded.readRows() - annualMovements.size()));
    }

    private <T> LoadedRows<T> readCsv(String path, RowMapper<T> mapper) throws IOException {
        List<T> validRows = new ArrayList<>();
        int readRows = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                readRows++;
                String[] columns = line.split(",", -1);
                try {
                    validRows.add(mapper.map(columns));
                } catch (RuntimeException ignored) {
                    // Invalid legacy rows are omitted to expose only consistent data through the BFFs.
                }
            }
        }
        return new LoadedRows<>(readRows, validRows);
    }

    private <T> Map<Object, T> deduplicate(List<T> rows, Function<T, Object> keyExtractor) {
        Map<Object, T> uniqueRows = new HashMap<>();
        for (T row : rows) {
            uniqueRows.putIfAbsent(keyExtractor.apply(row), row);
        }
        return uniqueRows;
    }

    private Optional<LocalDate> parseDate(String value) {
        String cleanValue = value == null ? "" : value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(cleanValue, formatter));
            } catch (DateTimeParseException ignored) {
                // Try next known legacy format.
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private record LoadedRows<T>(int readRows, List<T> validRows) {
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(String[] columns);
    }
}
