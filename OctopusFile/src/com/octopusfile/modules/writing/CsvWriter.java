package com.octopusfile.modules.writing;

import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.validation.FileValidator;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * Escritor de CSV compatível com RFC 4180.
 *
 * Escapa automaticamente campos que contenham o delimitador,
 * aspas duplas ou quebras de linha, envolvendo-os em aspas
 * e duplicando aspas internas.
 */
public class CsvWriter {

    private final NIO2FileSystem fileSystem;
    private final FileValidator validator;
    private final char delimiter;

    private static final String LINE_SEPARATOR = "\r\n";

    // ============================================================
    // CONSTRUTORES
    // ============================================================

    public CsvWriter() {
        this(',', new NIO2FileSystem(), new FileValidator());
    }

    public CsvWriter(
            char delimiter,
            NIO2FileSystem fileSystem,
            FileValidator validator
    ) {
        this.delimiter = delimiter;
        this.fileSystem = fileSystem;
        this.validator = validator;
    }

    // ============================================================
    // WRITE ROWS
    // ============================================================

    /**
     * Escreve uma lista de linhas, onde cada linha contém uma
     * lista de campos.
     */
    public Path writeRows(
            Path path,
            List<List<String>> rows
    ) {

        validator.validateForWrite(path);

        StringBuilder sb = new StringBuilder();

        for (List<String> row : rows) {
            appendRow(sb, row);
        }

        return fileSystem.writeString(
                path,
                sb.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // ============================================================
    // WRITE CSV
    // ============================================================

    /**
     * Escreve linhas CSV recebidas como arrays de String.
     *
     * Este método é uma alternativa conveniente para APIs
     * que trabalham com String[] em vez de List<String>.
     *
     * @param path caminho do arquivo CSV
     * @param linhas linhas do CSV
     * @param delimitador delimitador utilizado no arquivo
     * @return caminho do arquivo criado
     */
    public Path writeCsv(
            Path path,
            List<String[]> linhas,
            String delimitador
    ) {

        validator.validateForWrite(path);

        if (delimitador == null || delimitador.isEmpty()) {
            throw new IllegalArgumentException(
                    "O delimitador não pode ser nulo ou vazio"
            );
        }

        if (delimitador.length() != 1) {
            throw new IllegalArgumentException(
                    "O delimitador deve possuir exatamente um caractere"
            );
        }

        char csvDelimiter = delimitador.charAt(0);

        StringBuilder sb = new StringBuilder();

        if (linhas != null) {

            for (String[] linha : linhas) {

                if (linha == null) {
                    continue;
                }

                appendRow(
                        sb,
                        List.of(linha),
                        csvDelimiter
                );
            }
        }

        return fileSystem.writeString(
                path,
                sb.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // ============================================================
    // WRITE RECORDS
    // ============================================================

    /**
     * Escreve uma lista de registros Map em CSV.
     *
     * As chaves do primeiro registro são utilizadas como cabeçalho.
     */
    public Path writeRecords(
            Path path,
            List<Map<String, String>> records
    ) {

        validator.validateForWrite(path);

        if (records.isEmpty()) {

            return fileSystem.writeString(
                    path,
                    "",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }

        List<String> header =
                List.copyOf(records.get(0).keySet());

        StringBuilder sb = new StringBuilder();

        appendRow(sb, header);

        for (Map<String, String> record : records) {

            List<String> row = header.stream()
                    .map(col -> record.getOrDefault(col, ""))
                    .toList();

            appendRow(sb, row);
        }

        return fileSystem.writeString(
                path,
                sb.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // ============================================================
    // APPEND ROW
    // ============================================================

    private void appendRow(
            StringBuilder sb,List<String> fields
    ) {

        appendRow(sb, fields, delimiter);
    }

    private void appendRow(
            StringBuilder sb,List<String> fields,char currentDelimiter
    ) {

        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(currentDelimiter);
            }
            sb.append(escape(fields.get(i),currentDelimiter)
            );
        }
        sb.append(LINE_SEPARATOR);
    }

    // ============================================================
    // ESCAPE
    // ============================================================

    private String escape(String field) {
        return escape(field, delimiter);
    }

    private String escape(
            String field, char currentDelimiter
    ) {

        if (field == null) {
            return "";
        }

        boolean needsQuoting = field.indexOf(currentDelimiter) >= 0 || field.indexOf('"') >= 0 || field.indexOf('\n') >= 0
                        || field.indexOf('\r') >= 0;

        if (!needsQuoting) {
            return field;
        }

        return '"' +field.replace("\"", "\"\"") +'"';
    }
}