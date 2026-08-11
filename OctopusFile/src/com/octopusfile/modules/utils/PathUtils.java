package com.octopusfile.modules.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Funções utilitárias para manipulação de caminhos (paths).
 */
public final class PathUtils {

    private PathUtils() {
        // classe utilitária: não deve ser instanciada
    }

    public static String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return (index == -1) ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    public static Path combine(String first, String... more) {
        return Paths.get(first, more);
    }

    public static String getParentAsString(Path path) {
        Path parent = path.getParent();
        return parent == null ? "" : parent.toString();
    }

    public static boolean isAbsolute(Path path) {
        return path.isAbsolute();
    }

    public static Path normalize(Path path) {
        return path.normalize();
    }
}
