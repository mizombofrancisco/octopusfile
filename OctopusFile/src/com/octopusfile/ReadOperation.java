package com.octopusfile;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.modules.filtering.CsvFilter;
import com.octopusfile.modules.reading.CsvReader;
import com.octopusfile.modules.reading.FileReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Operação de leitura fluente, devolvida por {@link OctopusFile#read(String)}.
 * <p>
 * Permite compor a leitura de um arquivo com filtros e, por fim, consumir
 * ou coletar o resultado:
 * <pre>{@code
 * OctopusFile.of()
 *     .read("dados.csv")
 *     .filter(new CsvFilter().withDelimiter(";"))
 *     .forEach(System.out::println);
 * }</pre>
 * Não lança exceções checadas: qualquer erro de I/O vira uma
 * {@link OctopusFileException}.
 */
public class ReadOperation {

    private final Path path;
    private final FileReader fileReader = new FileReader();
    private final CsvReader csvReader = new CsvReader();

    private CsvFilter csvFilter;
    private Predicate<String> lineFilter;

    ReadOperation(Path path) {
        this.path = path;
    }

    /**
     * Aplica um filtro/configuração de CSV (define delimitador e,
     * opcionalmente, uma condição por linha já convertida em colunas).
     */
    public ReadOperation filter(CsvFilter csvFilter) {
        this.csvFilter = csvFilter;
        return this;
    }

    /** Aplica um filtro simples por linha de texto (antes de qualquer parsing). */
    public ReadOperation filter(Predicate<String> lineFilter) {
        this.lineFilter = lineFilter;
        return this;
    }

    /** Lê o arquivo como texto único (sem parsing de CSV nem filtros). */
    public String asText() {
        try {
            return fileReader.readAllText(path);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.READ_ERROR, e);
        }
    }

    /** Lê o arquivo como CSV (usando o delimitador do {@link CsvFilter}, se houver) e entrega cada linha já filtrada. */
    public void forEach(Consumer<String[]> action) {
        for (String[] linha : collect()) {
            action.accept(linha);
        }
    }

    /** Lê o arquivo como texto simples, linha a linha, aplicando o filtro de linha (se houver). */
    public void forEachLine(Consumer<String> action) {
        try {
            String conteudo = fileReader.readAllText(path);
            conteudo.lines()
                    .filter(linha -> lineFilter == null || lineFilter.test(linha))
                    .forEach(action);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.READ_ERROR, e);
        }
    }

    /** Coleta o resultado da leitura (CSV) em uma lista de colunas por linha. */
    public List<String[]> collect() {
        try {
            String delimitador = csvFilter != null ? csvFilter.getDelimiter() : ",";
            List<String[]> linhas = csvReader.readCsv(path, delimitador);
            List<String[]> resultado = new ArrayList<>();
            for (String[] linha : linhas) {
                if (csvFilter == null || csvFilter.test(linha)) {
                    resultado.add(linha);
                }
            }
            return resultado;
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.READ_ERROR, e);
        }
    }

    /** Transforma cada linha (texto) e coleta o resultado em uma lista. */
    public <T> List<T> map(Function<String, T> mapper) {
        try {
            String conteudo = fileReader.readAllText(path);
            return conteudo.lines()
                    .filter(linha -> lineFilter == null || lineFilter.test(linha))
                    .map(mapper)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.READ_ERROR, e);
        }
    }
}
