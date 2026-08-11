package com.octopusfile.modules.filtering;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Filtra arquivos por extensão (ex.: "txt", "jpg").
 */
public class TypeFilter implements FileFilter {

    private final String extension;

    public TypeFilter(String extension) {
        this.extension = extension.startsWith(".")
                ? extension.substring(1).toLowerCase(Locale.ROOT)
                : extension.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith("." + extension);
    }
}
