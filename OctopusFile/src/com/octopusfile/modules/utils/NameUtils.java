package com.octopusfile.modules.utils;

/**
 * Funções utilitárias para manipulação de nomes de arquivo.
 */
public final class NameUtils {

    private NameUtils() {
        // classe utilitária: não deve ser instanciada
    }

    public static String removeExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index == -1 ? fileName : fileName.substring(0, index);
    }

    public static String sanitize(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static String appendSuffix(String fileName, String suffix) {
        String base = removeExtension(fileName);
        String extension = fileName.substring(base.length());
        return base + suffix + extension;
    }
}
