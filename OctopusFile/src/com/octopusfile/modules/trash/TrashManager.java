package com.octopusfile.modules.trash;

import com.octopusfile.infrastructure.compression.CompressionEngine;
import com.octopusfile.infrastructure.compression.ZipCompressionEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Gerencia uma "lixeira": ao invés de apagar um arquivo/diretório
 * definitivamente, ele é comprimido e movido para {@code trashDirectory},
 * podendo ser restaurado depois — como o "Trash"/"Lixeira" do sistema
 * operacional, porém compactado para não consumir muito espaço.
 * <p>
 * Toda a leitura/escrita do conteúdo é feita em streaming (ver
 * {@link CompressionEngine}); apenas os metadados (caminho original, data,
 * tamanho) ficam residentes em memória.
 */
public class TrashManager {

    private final Path trashDirectory;
    private final CompressionEngine compressionEngine;
    private final TrashIndex index;

    public TrashManager(Path trashDirectory) {
        this(trashDirectory, new ZipCompressionEngine());
    }

    public TrashManager(Path trashDirectory, CompressionEngine compressionEngine) {
        this.trashDirectory = trashDirectory;
        this.compressionEngine = compressionEngine;
        this.index = new TrashIndex(trashDirectory);
    }

    /**
     * Move um arquivo ou diretório para a lixeira: comprime o conteúdo,
     * apaga o original e registra os metadados para uma futura restauração.
     *
     * @param path arquivo ou diretório a ser enviado para a lixeira
     * @return metadados do item recém-criado na lixeira (guarde o {@code id}
     *         para restaurar depois)
     * @throws IOException caso o item não possa ser movido para a lixeira
     */
    public TrashEntry moveToTrash(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Caminho não encontrado: " + path);
        }

        Files.createDirectories(trashDirectory);

        String id = UUID.randomUUID().toString();
        Path archivePath = trashDirectory.resolve(id + ".zip");
        boolean isDirectory = Files.isDirectory(path);
        long tamanhoOriginal = calculateSize(path);

        compressionEngine.compress(path, archivePath);
        deleteRecursively(path);

        TrashEntry entry = new TrashEntry(
                id,
                path.toAbsolutePath(),
                archivePath,
                Instant.now(),
                isDirectory,
                tamanhoOriginal,
                Files.size(archivePath)
        );

        index.append(entry);

        return entry;
    }

    /**
     * Restaura um item da lixeira para o seu caminho original.
     *
     * @param id identificador devolvido por {@link #moveToTrash(Path)}
     * @throws IOException caso o item não exista ou não possa ser restaurado
     */
    public void restore(String id) throws IOException {
        TrashEntry entry = index.find(id)
                .orElseThrow(() -> new IOException("Item não encontrado na lixeira: " + id));

        restore(id, entry.getOriginalPath());
    }

    /**
     * Restaura um item da lixeira para um caminho alternativo (permite
     * renomear ou mudar o local de destino).
     *
     * @param id          identificador devolvido por {@link #moveToTrash(Path)}
     * @param destination caminho (arquivo ou diretório) onde o conteúdo será restaurado
     * @throws IOException caso o item não exista ou não possa ser restaurado
     */
    public void restore(String id, Path destination) throws IOException {
        TrashEntry entry = index.find(id)
                .orElseThrow(() -> new IOException("Item não encontrado na lixeira: " + id));

        Path staging = Files.createTempDirectory("octopusfile-restore-");
        try {
            compressionEngine.decompress(entry.getArchivePath(), staging);

            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }

            if (entry.isDirectory()) {
                // O conteúdo do diretório original foi extraído solto dentro
                // de "staging" (sem pasta-raiz), então "staging" já É o
                // diretório restaurado — só falta movê-lo para o destino.
                moveTree(staging, destination);
            } else {
                try (Stream<Path> stream = Files.list(staging)) {
                    Path arquivoRestaurado = stream.findFirst()
                            .orElseThrow(() -> new IOException(
                                    "Backup da lixeira corrompido para o item: " + id));

                    Files.move(arquivoRestaurado, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } finally {
            deleteRecursivelyQuietly(staging);
        }

        Files.deleteIfExists(entry.getArchivePath());
        index.remove(id);
    }

    /** Lista todos os itens atualmente na lixeira. */
    public List<TrashEntry> list() throws IOException {
        return index.loadAll();
    }

    /** Apaga definitivamente um item da lixeira (sem possibilidade de restauração). */
    public void purge(String id) throws IOException {
        Optional<TrashEntry> entry = index.find(id);

        if (entry.isPresent()) {
            Files.deleteIfExists(entry.get().getArchivePath());
            index.remove(id);
        }
    }

    /** Esvazia a lixeira, apagando definitivamente todos os itens. */
    public void empty() throws IOException {
        for (TrashEntry entry : index.loadAll()) {
            Files.deleteIfExists(entry.getArchivePath());
        }
        index.clear();
    }

    private long calculateSize(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return Files.size(path);
        }

        try (Stream<Path> stream = Files.walk(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            return Files.size(file);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        }
    }

    /** Move cada arquivo de {@code source} para {@code destination}, funcionando mesmo entre sistemas de arquivos distintos. */
    private void moveTree(Path source, Path destination) throws IOException {
        Files.createDirectories(destination);

        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                Path relativo = source.relativize(path);
                if (relativo.toString().isEmpty()) {
                    continue;
                }

                Path alvo = destination.resolve(relativo);

                if (Files.isDirectory(path)) {
                    Files.createDirectories(alvo);
                } else {
                    if (alvo.getParent() != null) {
                        Files.createDirectories(alvo.getParent());
                    }
                    Files.move(path, alvo, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        deleteRecursively(source);
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        if (!Files.isDirectory(path)) {
            Files.delete(path);
            return;
        }

        try (Stream<Path> stream = Files.walk(path)) {
            List<Path> emOrdem = stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
            for (Path p : emOrdem) {
                Files.delete(p);
            }
        }
    }

    private void deleteRecursivelyQuietly(Path path) {
        try {
            deleteRecursively(path);
        } catch (IOException ignored) {
            // diretório temporário — melhor esforço
        }
    }
}
