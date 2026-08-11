package com.octopusfile.modules.organization;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrupa arquivos de um diretório em coleções, de acordo com um critério
 * (por padrão, o tipo/extensão do arquivo).
 */
public class FileGrouper {

    public Map<String, List<Path>> groupByType(Path directory) throws IOException {
        Map<String, List<Path>> grupos = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    String extensao = extractExtension(file);
                    grupos.computeIfAbsent(extensao, k -> new ArrayList<>()).add(file);
                }
            }
        }

        return grupos;
    }

    private String extractExtension(Path path) {
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return (index == -1) ? "sem_extensao" : fileName.substring(index + 1).toLowerCase();
    }
}
