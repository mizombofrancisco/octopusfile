package com.octopusfile.modules.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Funções utilitárias genéricas para verificação e manipulação de arquivos.
 */
public final class FileUtils {

    private FileUtils() {
        // classe utilitária: não deve ser instanciada
    }

    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    public static boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                return stream.findAny().isEmpty();
            }
        }
        return Files.size(path) == 0L;
    }

    public static boolean isReadable(Path path) {
        return Files.isReadable(path);
    }

    public static boolean isWritable(Path path) {
        return Files.isWritable(path);
    }

    public static boolean isHidden(Path path) throws IOException {
        return Files.isHidden(path);
    }
}
