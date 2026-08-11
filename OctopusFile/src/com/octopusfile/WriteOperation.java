package com.octopusfile;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.modules.writing.CsvWriter;
import com.octopusfile.modules.writing.FileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Operação de escrita fluente, devolvida por {@link OctopusFile#write(String)}.
 * <pre>{@code
 * OctopusFile.of().write("saida.txt").text("Olá mundo");
 * OctopusFile.of().write("dados.csv").csv(linhas, ";");
 * }</pre>
 * Não lança exceções checadas: qualquer erro de I/O vira uma
 * {@link OctopusFileException}, para não obrigar o uso de try/catch
 * em casos simples.
 */
public class WriteOperation {

    private final Path path;
    private final FileWriter fileWriter = new FileWriter();
    private final CsvWriter csvWriter = new CsvWriter();

    WriteOperation(Path path) {
        this.path = path;
    }

    /** Escreve texto no arquivo, substituindo o conteúdo existente. */
    public void text(String conteudo) {
        try {
            createDirectoriesIfNecessary();
            fileWriter.writeText(path, conteudo);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.WRITE_ERROR, e);
        }
    }

    /** Adiciona texto ao final do arquivo, sem apagar o conteúdo existente. */
    public void append(String conteudo) {
        try {
            createDirectoriesIfNecessary();
            Files.writeString(path, conteudo, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.WRITE_ERROR, e);
        }
    }

    /** Escreve bytes crus no arquivo (ex.: imagens, arquivos binários). */
    public void bytes(byte[] dados) {
        try {
            createDirectoriesIfNecessary();
            Files.write(path, dados, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.WRITE_ERROR, e);
        }
    }

    /** Escreve linhas como CSV, usando vírgula como delimitador. */
    public void csv(List<String[]> linhas) {
        csv(linhas, ",");
    }

    /** Escreve linhas como CSV, usando o delimitador informado. */
    public void csv(List<String[]> linhas, String delimitador) {
        try {
            createDirectoriesIfNecessary();
            csvWriter.writeCsv(path, linhas, delimitador);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.WRITE_ERROR, e);
        }
    }

    private void createDirectoriesIfNecessary() throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
    }
}
