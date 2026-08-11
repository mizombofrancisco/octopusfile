package com.octopusfile.modules.trash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Índice persistente da lixeira: guarda apenas metadados (uma entrada por
 * linha, sem o conteúdo dos arquivos), por isso pode ser lido/reescrito por
 * completo sem impacto relevante de memória, mesmo com muitos itens.
 */
class TrashIndex {

    private final Path indexFile;

    TrashIndex(Path trashDirectory) {
        this.indexFile = trashDirectory.resolve("index.tsv");
    }

    synchronized List<TrashEntry> loadAll() throws IOException {
        if (!Files.exists(indexFile)) {
            return new ArrayList<>();
        }

        List<TrashEntry> entradas = new ArrayList<>();
        for (String linha : Files.readAllLines(indexFile, StandardCharsets.UTF_8)) {
            if (!linha.isBlank()) {
                entradas.add(TrashEntry.fromLine(linha));
            }
        }
        return entradas;
    }

    synchronized Optional<TrashEntry> find(String id) throws IOException {
        return loadAll().stream()
                .filter(entry -> entry.getId().equals(id))
                .findFirst();
    }

    synchronized void append(TrashEntry entry) throws IOException {
        if (indexFile.getParent() != null) {
            Files.createDirectories(indexFile.getParent());
        }

        Files.writeString(
                indexFile,
                entry.toLine() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    synchronized void remove(String id) throws IOException {
        List<TrashEntry> restantes = new ArrayList<>();
        for (TrashEntry entry : loadAll()) {
            if (!entry.getId().equals(id)) {
                restantes.add(entry);
            }
        }
        rewrite(restantes);
    }

    synchronized void clear() throws IOException {
        rewrite(new ArrayList<>());
    }

    private void rewrite(List<TrashEntry> entradas) throws IOException {
        StringBuilder conteudo = new StringBuilder();
        for (TrashEntry entry : entradas) {
            conteudo.append(entry.toLine()).append(System.lineSeparator());
        }

        Files.writeString(
                indexFile,
                conteudo.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
