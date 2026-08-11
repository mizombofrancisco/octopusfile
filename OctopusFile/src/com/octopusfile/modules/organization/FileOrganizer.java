package com.octopusfile.modules.organization;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Organiza os arquivos de um diretório em subpastas de acordo com uma
 * estratégia de categorização (ex.: por extensão).
 */
public class FileOrganizer {

    private final FileCategorizer categorizer;

    public FileOrganizer() {
        this.categorizer = new FileCategorizer();
    }

    public FileOrganizer(FileCategorizer categorizer) {
        this.categorizer = categorizer;
    }

    /**
     * Move cada arquivo do diretório para uma subpasta nomeada de acordo
     * com a categoria retornada pelo {@link FileCategorizer}.
     */
    public void organize(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("O caminho informado não é um diretório: " + directory);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    String categoria = categorizer.categorize(file);
                    Path destino = directory.resolve(categoria);
                    Files.createDirectories(destino);
                    Files.move(file, destino.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
