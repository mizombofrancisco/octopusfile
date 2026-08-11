package com.octopusfile.modules.reading;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.validation.FileValidator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;


/**
 * Leitor de CSV compatível com RFC 4180: suporta campos entre aspas duplas
 * contendo vírgulas, quebras de linha e aspas escapadas ({@code ""}).
 * Não usa nenhuma dependência externa — parser implementado manualmente
 * caractere a caractere para manter a biblioteca livre de dependências
 * pesadas de parsing.
 */
public class CsvReader {

    private final NIO2FileSystem fileSystem;
    private final FileValidator validator;
    private final char delimiter;

    public CsvReader() {
        this(',', new NIO2FileSystem(), new FileValidator());
    }

    public CsvReader(char delimiter, NIO2FileSystem fileSystem, FileValidator validator) {
        this.delimiter = delimiter;
        this.fileSystem = fileSystem;
        this.validator = validator;
    }

    public List<String[]> readCsv(Path path, String delimiter) throws IOException {
        return Files.readAllLines(path).stream()
                .map(line -> line.split(delimiter))
                .toList();
    }


    /** Lê o CSV inteiro como lista de linhas, cada linha uma lista de campos (sem tratar cabeçalho). */
    public List<List<String>> readRows(Path path) {
        validator.validateExistingFile(path);
        String content = fileSystem.readString(path);
        return parse(content);
    }

    /**
     * Lê o CSV tratando a primeira linha como cabeçalho, retornando cada
     * linha subsequente como um Map ordenado (nome da coluna -> valor).
     */
    public List<Map<String, String>> readAsRecords(Path path) {
        List<List<String>> rows = readRows(path);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> header = rows.get(0);
        List<Map<String, String>> records = new ArrayList<>(rows.size() - 1);
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            Map<String, String> record = new LinkedHashMap<>();
            for (int col = 0; col < header.size(); col++) {
                record.put(header.get(col), col < row.size() ? row.get(col) : "");
            }
            records.add(record);
        }
        return records;
    }

    private List<List<String>> parse(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder field = new StringBuilder();

        boolean inQuotes = false;
        int i = 0;
        int len = content.length();

        while (i < len) {
            char c = content.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                    continue;
                }
                field.append(c);
                i++;
                continue;
            }

            switch (c) {
                case '"' -> {
                    if (field.length() > 0) {
                        throw new OctopusFileException(ErrorCodes.INVALID_ARGUMENT,
                                "Aspas inesperadas no meio de um campo não citado (posição " + i + ")");
                    }
                    inQuotes = true;
                }
                case '\r' -> { /* ignorado; \n trata a quebra de linha */ }
                case '\n' -> {
                    currentRow.add(field.toString());
                    field.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                }
                default -> {
                    if (c == delimiter) {
                        currentRow.add(field.toString());
                        field.setLength(0);
                    } else {
                        field.append(c);
                    }
                }
            }
            i++;
        }

        // última linha (sem \n final)
        if (field.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(field.toString());
            rows.add(currentRow);
        }

        if (inQuotes) {
            throw new OctopusFileException(ErrorCodes.INVALID_ARGUMENT, "Campo entre aspas não foi fechado no CSV");
        }

        return rows;
    }
}
