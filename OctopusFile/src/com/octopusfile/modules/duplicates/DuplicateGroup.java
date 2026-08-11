package com.octopusfile.modules.duplicates;

import java.nio.file.Path;
import java.util.List;

/**
 * Um grupo de arquivos com conteúdo idêntico (mesmo hash).
 */
public class DuplicateGroup {

    private final String hash;
    private final List<Path> files;
    private final long fileSizeBytes;

    public DuplicateGroup(String hash, List<Path> files, long fileSizeBytes) {
        this.hash = hash;
        this.files = files;
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getHash() {
        return hash;
    }

    /** Todos os arquivos deste grupo, incluindo o "original" (o primeiro encontrado). */
    public List<Path> getFiles() {
        return files;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    /** Espaço em disco que seria recuperado apagando todas as cópias, mantendo apenas uma. */
    public long getWastedBytes() {
        return fileSizeBytes * (files.size() - 1L);
    }

    @Override
    public String toString() {
        return "DuplicateGroup{hash='" + hash + '\'' +
                ", files=" + files +
                ", wastedBytes=" + getWastedBytes() +
                '}';
    }
}
