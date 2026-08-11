package com.octopusfile.modules.organization;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordena os arquivos de um diretório de acordo com diferentes critérios.
 */
public class FileSorter {

    public List<Path> sortByName(Path directory) throws IOException {
        List<Path> files = listFiles(directory);
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return files;
    }

    public List<Path> sortBySize(Path directory) throws IOException {
        List<Path> files = listFiles(directory);
        files.sort(Comparator.comparingLong(this::sizeOf));
        return files;
    }

    public List<Path> sortByLastModified(Path directory) throws IOException {
        List<Path> files = listFiles(directory);
        files.sort(Comparator.comparingLong(this::lastModifiedOf));
        return files;
    }

    private long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private long lastModifiedOf(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private List<Path> listFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    files.add(path);
                }
            }
        }
        return files;
    }
}
