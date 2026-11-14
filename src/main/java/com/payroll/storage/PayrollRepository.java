package com.payroll.storage;

import com.payroll.model.PayrollRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe CSV persistence repository for Historical Payroll Records.
 * 
 * Historical Integrity Guarantees:
 * - Every record is saved as an immutable, self-contained snapshot.
 * - Changing an employee's designation or a designation policy later has ZERO effect on historical records.
 * - Enforces duplicate payroll prevention for the same employee and pay period.
 */
public class PayrollRepository {

    public static final List<String> HEADERS = List.of(
            "record_id", "employee_id", "employee_name", "designation", "department",
            "employee_type", "work_location", "doj", "pf_account_number", "uan",
            "pay_period", "pay_date", "total_working_days", "present_days", "absent_days",
            "project_name", "eligible_project_days", "base_rate", "payable_basic",
            "payable_hra", "payable_da", "payable_conveyance", "other_earnings",
            "gross_earnings", "pf_employee", "pf_employer", "professional_tax",
            "other_deductions", "total_deductions", "net_salary", "policy_snapshot",
            "generation_date", "status", "original_record_id", "correction_reason",
            "correction_timestamp"
    );

    private final Path filePath;
    private final Map<String, PayrollRecord> cache = new ConcurrentHashMap<>();

    public PayrollRepository() {
        this(Paths.get("data", "payroll.csv"));
    }

    public PayrollRepository(Path filePath) {
        this.filePath = filePath;
        initFile();
    }

    private void initFile() {
        try {
            if (Files.exists(filePath) && Files.size(filePath) > 0) {
                loadAll();
            } else {
                Path parent = filePath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                // Write headers to ensure empty payroll file exists with valid schema
                FileHandler.writeCsvAtomic(filePath, HEADERS, Collections.emptyList());
            }
        } catch (IOException e) {
            System.err.println("Warning: Error initializing payroll repository: " + e.getMessage());
        }
    }

    /**
     * Strictly verifies whether an active payroll record already exists for the employee and pay period.
     * Superseded records from authorized corrections are excluded from duplicate blocks.
     */
    public boolean exists(String employeeId, String payPeriod) {
        if (employeeId == null || payPeriod == null) return false;
        String cleanEmpId = employeeId.trim().toUpperCase();
        String cleanPeriod = payPeriod.trim();

        return cache.values().stream().anyMatch(rec ->
                !rec.isSuperseded() &&
                rec.getEmployeeId().equalsIgnoreCase(cleanEmpId) &&
                rec.getPayPeriod().equalsIgnoreCase(cleanPeriod)
        );
    }

    /**
     * Saves a new historical payroll record.
     * Prevents duplicate payroll generation for the same employee and period.
     */
    public synchronized void save(PayrollRecord record) throws IOException {
        if (record == null || record.getEmployeeId() == null || record.getPayPeriod() == null) {
            throw new IllegalArgumentException("Payroll record must have a valid employee ID and pay period.");
        }

        if (exists(record.getEmployeeId(), record.getPayPeriod())) {
            throw new IllegalStateException("Duplicate Payroll Processing: Payroll has already been generated for Employee '" +
                    record.getEmployeeId() + "' in period '" + record.getPayPeriod() + "'.");
        }

        cache.put(record.getRecordId(), record);

        // Append line directly to CSV or rewrite safely
        saveAllToDisk();
    }

    /**
     * Explicit authorized correction workflow for an existing payroll record.
     * Archives the original record as SUPERSEDED and saves the new record as CORRECTED.
     * Ensures immutable audit trail without silently overwriting historical data.
     */
    public synchronized void saveCorrection(PayrollRecord originalRecord, PayrollRecord correctedRecord) throws IOException {
        if (originalRecord == null || correctedRecord == null) {
            throw new IllegalArgumentException("Original and corrected records cannot be null.");
        }
        if (!cache.containsKey(originalRecord.getRecordId())) {
            throw new IllegalArgumentException("Cannot correct non-existent payroll record: " + originalRecord.getRecordId());
        }
        if (originalRecord.isSuperseded() || "SUPERSEDED".equalsIgnoreCase(originalRecord.getStatus())) {
            throw new IllegalArgumentException("Cannot correct an already superseded payroll record: " + originalRecord.getRecordId());
        }

        String prevStatus = originalRecord.getStatus();
        originalRecord.setStatus("SUPERSEDED");
        correctedRecord.setStatus("CORRECTED");
        correctedRecord.setOriginalRecordId(originalRecord.getRecordId());
        if (correctedRecord.getCorrectionTimestamp() == null || correctedRecord.getCorrectionTimestamp().isEmpty()) {
            correctedRecord.setCorrectionTimestamp(LocalDate.now().toString());
        }

        cache.put(originalRecord.getRecordId(), originalRecord);
        cache.put(correctedRecord.getRecordId(), correctedRecord);
        try {
            saveAllToDisk();
        } catch (Exception e) {
            // Rollback in-memory state to guarantee transactional integrity
            originalRecord.setStatus(prevStatus);
            cache.put(originalRecord.getRecordId(), originalRecord);
            cache.remove(correctedRecord.getRecordId());
            throw e;
        }
    }

    /**
     * Legacy updateCorrection method preserved for backwards compatibility.
     */
    public synchronized void updateCorrection(PayrollRecord record) throws IOException {
        if (record == null || record.getRecordId() == null) {
            throw new IllegalArgumentException("Payroll record must have a valid record ID for correction.");
        }
        if (!cache.containsKey(record.getRecordId())) {
            throw new IllegalArgumentException("Cannot correct non-existent payroll record: " + record.getRecordId());
        }
        cache.put(record.getRecordId(), record);
        saveAllToDisk();
    }

    public synchronized List<PayrollRecord> loadAll() {
        try {
            List<List<String>> rows = FileHandler.readCsv(filePath);
            cache.clear();

            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (i == 0 && !row.isEmpty() && row.get(0).equalsIgnoreCase("record_id")) {
                    continue; // Skip CSV header
                }
                if (row.size() < 30) continue; // Skip malformed rows safely

                PayrollRecord record = mapRowToRecord(row);
                if (record != null) {
                    cache.put(record.getRecordId(), record);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading payroll history from CSV: " + e.getMessage());
        }
        return new ArrayList<>(cache.values());
    }

    public Optional<PayrollRecord> findById(String recordId) {
        if (recordId == null) return Optional.empty();
        return Optional.ofNullable(cache.get(recordId));
    }

    public List<PayrollRecord> findByEmployeeId(String employeeId) {
        if (employeeId == null) return Collections.emptyList();
        String target = employeeId.trim().toUpperCase();
        return cache.values().stream()
                .filter(r -> r.getEmployeeId().equalsIgnoreCase(target))
                .collect(Collectors.toList());
    }

    public List<PayrollRecord> filter(String query, String payPeriod, String employeeType) {
        return filter(query, payPeriod, employeeType, "All");
    }

    public List<PayrollRecord> filter(String query, String payPeriod, String employeeType, String status) {
        return cache.values().stream()
                .filter(r -> {
                    if (query != null && !query.trim().isEmpty()) {
                        String q = query.trim().toLowerCase();
                        boolean matchId = r.getEmployeeId().toLowerCase().contains(q);
                        boolean matchName = r.getEmployeeNameSnapshot().toLowerCase().contains(q);
                        boolean matchRecId = r.getRecordId().toLowerCase().contains(q);
                        if (!matchId && !matchName && !matchRecId) return false;
                    }
                    if (payPeriod != null && !payPeriod.equalsIgnoreCase("All") && !payPeriod.equalsIgnoreCase("All Periods") && !payPeriod.trim().isEmpty()) {
                        if (!r.getPayPeriod().equalsIgnoreCase(payPeriod.trim())) return false;
                    }
                    if (employeeType != null && !employeeType.equalsIgnoreCase("All") && !employeeType.equalsIgnoreCase("All Categories") && !employeeType.trim().isEmpty()) {
                        if (!r.getEmployeeType().equalsIgnoreCase(employeeType.trim())) return false;
                    }
                    if (status != null && !status.equalsIgnoreCase("All") && !status.equalsIgnoreCase("All Statuses") && !status.trim().isEmpty()) {
                        if (!r.getStatus().equalsIgnoreCase(status.trim())) return false;
                    }
                    return true;
                })
                .sorted((a, b) -> {
                    int dateCmp = b.getGenerationDate().compareTo(a.getGenerationDate());
                    return dateCmp != 0 ? dateCmp : b.getRecordId().compareTo(a.getRecordId());
                })
                .collect(Collectors.toList());
    }

    protected void saveAllToDisk() {
        List<List<String>> rows = new ArrayList<>();
        for (PayrollRecord rec : cache.values()) {
            rows.add(mapRecordToRow(rec));
        }
        try {
            FileHandler.writeCsvAtomic(filePath, HEADERS, rows);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist payroll records to CSV: " + e.getMessage(), e);
        }
    }

    private PayrollRecord mapRowToRecord(List<String> row) {
        try {
            PayrollRecord r = new PayrollRecord();
            r.setRecordId(row.get(0).trim());
            r.setEmployeeId(row.get(1).trim());
            r.setEmployeeNameSnapshot(row.get(2).trim());
            r.setDesignationSnapshot(row.get(3).trim());
            r.setDepartment(row.get(4).trim());
            r.setEmployeeType(row.get(5).trim());
            r.setWorkLocation(row.get(6).trim());
            r.setDateOfJoining(LocalDate.parse(row.get(7).trim()));
            r.setPfAccountNumber(row.get(8).trim());
            r.setUan(row.get(9).trim());
            r.setPayPeriod(row.get(10).trim());
            r.setPayDate(LocalDate.parse(row.get(11).trim()));

            r.setTotalWorkingDays(Integer.parseInt(row.get(12).trim()));
            r.setPresentDays(Integer.parseInt(row.get(13).trim()));
            r.setAbsentDays(Integer.parseInt(row.get(14).trim()));

            r.setProjectName(row.get(15).trim());
            r.setEligibleProjectDays(Integer.parseInt(row.get(16).trim()));

            r.setBasicSalaryOrDailyRate(new BigDecimal(row.get(17).trim()));
            r.setPayableBasic(new BigDecimal(row.get(18).trim()));
            r.setPayableHra(new BigDecimal(row.get(19).trim()));
            r.setPayableDa(new BigDecimal(row.get(20).trim()));
            r.setPayableConveyance(new BigDecimal(row.get(21).trim()));
            r.setOtherEarnings(new BigDecimal(row.get(22).trim()));
            r.setGrossEarnings(new BigDecimal(row.get(23).trim()));

            r.setPfEmployeeDeduction(new BigDecimal(row.get(24).trim()));
            r.setPfEmployerContribution(new BigDecimal(row.get(25).trim()));
            r.setProfessionalTax(new BigDecimal(row.get(26).trim()));
            r.setOtherDeductions(new BigDecimal(row.get(27).trim()));
            r.setTotalDeductions(new BigDecimal(row.get(28).trim()));

            r.setNetSalary(new BigDecimal(row.get(29).trim()));

            if (row.size() > 30) r.setPolicySnapshot(row.get(30).trim());
            if (row.size() > 31) r.setGenerationDate(LocalDate.parse(row.get(31).trim()));
            if (row.size() > 32) r.setStatus(row.get(32).trim()); else r.setStatus("ORIGINAL");
            if (row.size() > 33) r.setOriginalRecordId(row.get(33).trim());
            if (row.size() > 34) r.setCorrectionReason(row.get(34).trim());
            if (row.size() > 35) r.setCorrectionTimestamp(row.get(35).trim());

            return r;
        } catch (Exception e) {
            System.err.println("Warning: Skipping corrupted payroll record: " + row + " (Error: " + e.getMessage() + ")");
            return null;
        }
    }

    private List<String> mapRecordToRow(PayrollRecord r) {
        List<String> row = new ArrayList<>();
        row.add(r.getRecordId());
        row.add(r.getEmployeeId());
        row.add(r.getEmployeeNameSnapshot());
        row.add(r.getDesignationSnapshot());
        row.add(r.getDepartment());
        row.add(r.getEmployeeType());
        row.add(r.getWorkLocation());
        row.add(r.getDateOfJoining() != null ? r.getDateOfJoining().toString() : "");
        row.add(r.getPfAccountNumber());
        row.add(r.getUan());
        row.add(r.getPayPeriod());
        row.add(r.getPayDate() != null ? r.getPayDate().toString() : "");
        row.add(String.valueOf(r.getTotalWorkingDays()));
        row.add(String.valueOf(r.getPresentDays()));
        row.add(String.valueOf(r.getAbsentDays()));
        row.add(r.getProjectName() != null ? r.getProjectName() : "");
        row.add(String.valueOf(r.getEligibleProjectDays()));
        row.add(r.getBasicSalaryOrDailyRate().toString());
        row.add(r.getPayableBasic().toString());
        row.add(r.getPayableHra().toString());
        row.add(r.getPayableDa().toString());
        row.add(r.getPayableConveyance().toString());
        row.add(r.getOtherEarnings().toString());
        row.add(r.getGrossEarnings().toString());
        row.add(r.getPfEmployeeDeduction().toString());
        row.add(r.getPfEmployerContribution().toString());
        row.add(r.getProfessionalTax().toString());
        row.add(r.getOtherDeductions().toString());
        row.add(r.getTotalDeductions().toString());
        row.add(r.getNetSalary().toString());
        row.add(r.getPolicySnapshot() != null ? r.getPolicySnapshot() : "");
        row.add(r.getGenerationDate() != null ? r.getGenerationDate().toString() : "");
        row.add(r.getStatus());
        row.add(r.getOriginalRecordId());
        row.add(r.getCorrectionReason());
        row.add(r.getCorrectionTimestamp());
        return row;
    }

    public Path getFilePath() {
        return filePath;
    }
}
