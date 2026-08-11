package com.octopusfile.modules.trash;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * Representa um item armazenado na lixeira. Os metadados permitem localizar
 * o arquivo comprimido e restaurá-lo ao caminho original (ou a outro, se
 * desejado).
 */
public class TrashEntry {

    private final String id;
    private final Path originalPath;
    private final Path archivePath;
    private final Instant deletedAt;
    private final boolean directory;
    private final long originalSizeBytes;
    private final long compressedSizeBytes;

    public TrashEntry(
            String id,
            Path originalPath,
            Path archivePath,
            Instant deletedAt,
            boolean directory,
            long originalSizeBytes,
            long compressedSizeBytes
    ) {
        this.id = id;
        this.originalPath = originalPath;
        this.archivePath = archivePath;
        this.deletedAt = deletedAt;
        this.directory = directory;
        this.originalSizeBytes = originalSizeBytes;
        this.compressedSizeBytes = compressedSizeBytes;
    }

    public String getId() {
        return id;
    }

    public Path getOriginalPath() {
        return originalPath;
    }

    public Path getArchivePath() {
        return archivePath;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDirectory() {
        return directory;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public long getCompressedSizeBytes() {
        return compressedSizeBytes;
    }

    /** Percentual de redução de tamanho obtido pela compressão (0–100). */
    public double getCompressionRatio() {
        if (originalSizeBytes == 0) {
            return 0.0;
        }
        return 100.0 * (1.0 - ((double) compressedSizeBytes / (double) originalSizeBytes));
    }

    /** Serializa a entrada em uma linha de texto (usada pelo {@link TrashIndex}). */
    String toLine() {
        return String.join(
                "\t",
                id,
                originalPath.toString(),
                archivePath.toString(),
                String.valueOf(deletedAt.toEpochMilli()),
                directory ? "D" : "F",
                String.valueOf(originalSizeBytes),
                String.valueOf(compressedSizeBytes)
        );
    }

    /** Reconstrói uma entrada a partir de uma linha gravada por {@link #toLine()}. */
    static TrashEntry fromLine(String linha) {
        String[] campos = linha.split("\t");
        return new TrashEntry(
                campos[0],
                Paths.get(campos[1]),
                Paths.get(campos[2]),
                Instant.ofEpochMilli(Long.parseLong(campos[3])),
                "D".equals(campos[4]),
                Long.parseLong(campos[5]),
                Long.parseLong(campos[6])
        );
    }

    @Override
    public String toString() {
        return "TrashEntry{" +
                "id='" + id + '\'' +
                ", originalPath=" + originalPath +
                ", deletedAt=" + deletedAt +
                ", directory=" + directory +
                ", originalSizeBytes=" + originalSizeBytes +
                ", compressedSizeBytes=" + compressedSizeBytes +
                '}';
    }
}
