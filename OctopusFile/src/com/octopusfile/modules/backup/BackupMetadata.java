package com.octopusfile.modules.backup;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Metadados resultantes de uma operação de backup: onde o backup foi salvo,
 * quando, e qual foi a economia de espaço obtida pela compressão.
 */
public class BackupMetadata {

    private final Path originalPath;
    private final Path backupPath;
    private final Instant createdAt;
    private final long originalSizeBytes;
    private final long compressedSizeBytes;

    public BackupMetadata(
            Path originalPath,
            Path backupPath,
            Instant createdAt,
            long originalSizeBytes,
            long compressedSizeBytes
    ) {
        this.originalPath = originalPath;
        this.backupPath = backupPath;
        this.createdAt = createdAt;
        this.originalSizeBytes = originalSizeBytes;
        this.compressedSizeBytes = compressedSizeBytes;
    }

    public Path getOriginalPath() {
        return originalPath;
    }

    public Path getBackupPath() {
        return backupPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
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

    @Override
    public String toString() {
        return "BackupMetadata{" +
                "originalPath=" + originalPath +
                ", backupPath=" + backupPath +
                ", createdAt=" + createdAt +
                ", originalSizeBytes=" + originalSizeBytes +
                ", compressedSizeBytes=" + compressedSizeBytes +
                ", compressionRatio=" + String.format("%.1f%%", getCompressionRatio()) +
                '}';
    }
}
