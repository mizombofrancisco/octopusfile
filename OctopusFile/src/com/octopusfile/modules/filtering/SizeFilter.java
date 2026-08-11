package com.octopusfile.modules.filtering;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Filtra arquivos com base no tamanho (em bytes).
 * Pode ser configurado com um tamanho mínimo, máximo, ou ambos.
 */
public class SizeFilter implements FileFilter {

    private final long minSize;
    private final long maxSize;

    /**
     * Filtro que aceita arquivos com tamanho até {@code maxSize} bytes.
     */
    public SizeFilter(long maxSize) {
        this(0L, maxSize);
    }

    public SizeFilter(long minSize, long maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    @Override
    public boolean accept(Path path) {
        try {
            long size = Files.size(path);
            return size >= minSize && size <= maxSize;
        } catch (IOException e) {
            return false;
        }
    }
}
