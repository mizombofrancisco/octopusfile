package com.octopusfile.modules.filtering;

import java.nio.file.Path;

/**
 * Contrato para filtros de arquivo utilizados na biblioteca.
 * Implementações decidem se um {@link Path} deve ou não ser aceito.
 */
public interface FileFilter {

    boolean accept(Path path);

    /**
     * Combina este filtro com outro usando uma operação lógica "E".
     */
    default FileFilter and(FileFilter other) {
        return path -> this.accept(path) && other.accept(path);
    }

    /**
     * Combina este filtro com outro usando uma operação lógica "OU".
     */
    default FileFilter or(FileFilter other) {
        return path -> this.accept(path) || other.accept(path);
    }

    /**
     * Retorna a negação deste filtro.
     */
    default FileFilter negate() {
        return path -> !this.accept(path);
    }
}
