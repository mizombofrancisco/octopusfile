package com.octopusfile.modules.sync;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumo do que aconteceu durante uma {@link DirectorySynchronizer#sync}.
 */
public class SyncResult {

    private final List<Path> copied = new ArrayList<>();
    private final List<Path> updated = new ArrayList<>();
    private final List<Path> deleted = new ArrayList<>();

    public List<Path> getCopied() {
        return copied;
    }

    public List<Path> getUpdated() {
        return updated;
    }

    public List<Path> getDeleted() {
        return deleted;
    }

    void addCopied(Path path) {
        copied.add(path);
    }

    void addUpdated(Path path) {
        updated.add(path);
    }

    void addDeleted(Path path) {
        deleted.add(path);
    }

    @Override
    public String toString() {
        return "SyncResult{" +
                "copiados=" + copied.size() +
                ", atualizados=" + updated.size() +
                ", removidos=" + deleted.size() +
                '}';
    }
}
