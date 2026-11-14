package com.payroll.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust, RFC-4180 compliant CSV file handling utility with safe atomic write guarantees.
 * 
 * Features:
 * - Proper escaping for commas, double quotes (" -> ""), and newlines.
 * - UTF-8 character encoding throughout.
 * - Atomic write operations using temporary files to prevent data corruption during crashes or interrupts.
 * - Graceful handling of missing directories, empty files, and malformed rows.
 */
public class FileHandler {

    /**
     * Reads all records from a CSV file. Returns empty list if file doesn't exist or is empty.
     */
    public static synchronized List<List<String>> readCsv(Path filePath) throws IOException {
        if (filePath == null || !Files.exists(filePath)) {
            return new ArrayList<>();
        }

        List<List<String>> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            StringBuilder multiLineBuffer = new StringBuilder();
            boolean insideQuotes = false;

            while ((line = reader.readLine()) != null) {
                if (insideQuotes) {
                    multiLineBuffer.append("\n").append(line);
                } else {
                    multiLineBuffer.setLength(0);
                    multiLineBuffer.append(line);
                }

                // Count non-escaped quotes to detect multi-line records
                insideQuotes = isInsideQuotes(multiLineBuffer.toString());
                if (!insideQuotes) {
                    List<String> fields = parseCsvLine(multiLineBuffer.toString());
                    if (!fields.isEmpty()) {
                        records.add(fields);
                    }
                }
            }
        }
        return records;
    }

    /**
     * Atomically writes records to a CSV file (replaces existing file safely).
     */
    public static synchronized void writeCsvAtomic(Path filePath, List<String> header, List<List<String>> rows) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null.");
        }

        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Path tempFile = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp." + System.nanoTime());

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            if (header != null && !header.isEmpty()) {
                writer.write(formatCsvLine(header));
                writer.newLine();
            }

            if (rows != null) {
                for (List<String> row : rows) {
                    writer.write(formatCsvLine(row));
                    writer.newLine();
                }
            }
            writer.flush();
        }

        try {
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (Files.exists(tempFile)) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Appends a single record to an existing CSV file, writing header first if file is newly created.
     */
    public static synchronized void appendCsvLine(Path filePath, List<String> header, List<String> row) throws IOException {
        if (filePath == null || row == null) return;

        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        boolean fileExists = Files.exists(filePath) && Files.size(filePath) > 0;

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (!fileExists && header != null && !header.isEmpty()) {
                writer.write(formatCsvLine(header));
                writer.newLine();
            }
            writer.write(formatCsvLine(row));
            writer.newLine();
            writer.flush();
        }
    }

    /**
     * Formats a list of field values into an RFC-4180 compliant CSV line.
     */
    public static String formatCsvLine(List<String> fields) {
        if (fields == null || fields.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeCsvField(fields.get(i)));
        }
        return sb.toString();
    }

    /**
     * Escapes a single field value according to RFC-4180 rules.
     */
    public static String escapeCsvField(String value) {
        if (value == null) return "";
        boolean containsComma = value.contains(",");
        boolean containsQuote = value.contains("\"");
        boolean containsNewline = value.contains("\n") || value.contains("\r");

        if (containsComma || containsQuote || containsNewline) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Parses an RFC-4180 compliant CSV line into individual string fields.
     */
    public static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        if (line == null) return fields;

        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;
        int len = line.length();

        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (insideQuotes && i + 1 < len && line.charAt(i + 1) == '"') {
                    // Escaped quote ("") inside quoted string
                    currentField.append('"');
                    i++; // skip next quote
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (c == ',' && !insideQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());
        return fields;
    }

    private static boolean isInsideQuotes(String s) {
        boolean inside = false;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"') {
                if (inside && i + 1 < s.length() && s.charAt(i + 1) == '"') {
                    i++;
                } else {
                    inside = !inside;
                }
            }
        }
        return inside;
    }
}

