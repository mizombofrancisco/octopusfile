package com.octopusfile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.Deflater;

/**
 * Configurações globais da biblioteca (log, comportamento padrão, lixeira,
 * backup, etc).
 */
public class GlobalConfiguration {

    private boolean isLoggingActive = true;
    private boolean isOverwriteByDefault = false;

    /** Diretório padrão onde os itens apagados via lixeira são armazenados (comprimidos). */
    private Path trashDirectory = Paths.get(".octopusfile", "trash");

    /** Diretório padrão onde os backups são armazenados quando nenhum destino é informado. */
    private Path backupDirectory = Paths.get(".octopusfile", "backup");

    /** Nível de compressão (0 = sem compressão / mais rápido, 9 = compressão máxima). */
    private int compressionLevel = Deflater.BEST_COMPRESSION;

    public boolean isLoggingActive() {
        return isLoggingActive;
    }

    public void setIsLoggingActive(boolean isLoggingActive) {
        this.isLoggingActive = isLoggingActive;
    }

    public boolean isOverwriteByDefault() {
        return isOverwriteByDefault;
    }

    public void setIsOverwriteByDefault(boolean isOverwriteByDefault) {
        this.isOverwriteByDefault = isOverwriteByDefault;
    }

    public Path getTrashDirectory() {
        return trashDirectory;
    }

    public void setTrashDirectory(Path trashDirectory) {
        this.trashDirectory = trashDirectory;
    }

    public Path getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(Path backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    /**
     * Define o nível de compressão usado por backups e pela lixeira.
     *
     * @param compressionLevel valor entre {@link Deflater#NO_COMPRESSION} (0)
     *                         e {@link Deflater#BEST_COMPRESSION} (9)
     */
    public void setCompressionLevel(int compressionLevel) {
        if (compressionLevel < Deflater.DEFAULT_COMPRESSION || compressionLevel > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException(
                    "Nível de compressão deve estar entre 0 e 9: " + compressionLevel
            );
        }
        this.compressionLevel = compressionLevel;
    }
}
