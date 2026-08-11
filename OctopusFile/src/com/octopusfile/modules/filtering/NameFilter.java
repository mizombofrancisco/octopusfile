package com.octopusfile.modules.filtering;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Filtra arquivos cujo nome contém um determinado trecho, ou que
 * correspondem a uma expressão regular (quando {@code useRegex} é true).
 */
public class NameFilter implements FileFilter {

    private final String pattern;
    private final boolean useRegex;

    public NameFilter(String pattern) {
        this(pattern, false);
    }

    public NameFilter(String pattern, boolean useRegex) {
        this.pattern = pattern;
        this.useRegex = useRegex;
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.getFileName().toString();
        if (useRegex) {
            return Pattern.matches(pattern, fileName);
        }
        return fileName.contains(pattern);
    }
}
