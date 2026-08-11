package com.octopusfile;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.modules.backup.BackupMetadata;
import com.octopusfile.modules.backup.BackupService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Operação de backup fluente, devolvida por {@link OctopusFile#backup(String)}.
 * <pre>{@code
 * BackupMetadata metadados = OctopusFile.of()
 *     .backup("relatorios/")
 *     .to("backups/");
 *
 * OctopusFile.of()
 *     .backup(metadados.getBackupPath())
 *     .restoreTo("relatorios-restaurados/");
 * }</pre>
 * O conteúdo é comprimido (ZIP) em modo streaming, sem sobrecarregar a
 * memória mesmo para diretórios grandes.
 */
public class BackupOperation {

    private final Path source;
    private final BackupService backupService = new BackupService();

    BackupOperation(Path source) {
        this.source = source;
    }

    /** Cria o backup comprimido de {@code source} dentro do diretório informado. */
    public BackupMetadata to(String backupDirectory) {
        return to(Paths.get(backupDirectory));
    }

    public BackupMetadata to(Path backupDirectory) {
        try {
            return backupService.backup(source, backupDirectory);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao criar backup de " + source),
                    e
            );
        }
    }

    /** Restaura um backup (arquivo .zip) previamente criado para o destino informado. */
    public void restoreTo(String destino) {
        restoreTo(Paths.get(destino));
    }

    public void restoreTo(Path destino) {
        try {
            backupService.restore(source, destino);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao restaurar backup de " + source),
                    e
            );
        }
    }
}
