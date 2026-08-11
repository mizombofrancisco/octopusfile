package com.octopusfile.modules.manipulation;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.logging.AuditLogger;
import com.octopusfile.support.logging.OperationLogger;
import com.octopusfile.support.security.AccessValidator;
import com.octopusfile.support.security.AccessValidator.Operation;

import java.nio.file.Path;

/**
 * Exclui arquivos e diretórios. É a operação mais perigosa do módulo de
 * manipulação (destrutiva e, para diretórios, potencialmente recursiva),
 * por isso:
 *  - sempre passa por AccessValidator, respeitando caminhos protegidos;
 *  - toda exclusão bem-sucedida é registrada em AuditLogger, não apenas
 *    em OperationLogger;
 *  - exclusão recursiva exige confirmação explícita via parâmetro
 *    {@code recursive=true} — nunca é o padrão implícito.
 */
public class FileDeleter {

    private final NIO2FileSystem fileSystem;
    private final AccessValidator accessValidator;
    private final OperationLogger operationLogger;
    private final AuditLogger auditLogger;

    public FileDeleter() {
        this(new NIO2FileSystem(), new AccessValidator(), new OperationLogger(), new AuditLogger());
    }

    public FileDeleter(NIO2FileSystem fileSystem, AccessValidator accessValidator,
                        OperationLogger operationLogger, AuditLogger auditLogger) {
        this.fileSystem = fileSystem;
        this.accessValidator = accessValidator;
        this.operationLogger = operationLogger;
        this.auditLogger = auditLogger;
    }

    /** Exclui um único arquivo. Lança FILE_NOT_FOUND se não existir. */
    public void deleteFile(Path path) {
        deleteFile(path, "system");
    }

    public void deleteFile(Path path, String actor) {
        long start = System.currentTimeMillis();
        operationLogger.started("DELETE_FILE", path);
        try {
            if (!fileSystem.isRegularFile(path)) {
                throw new OctopusFileException(ErrorCodes.FILE_NOT_FOUND, null, path);
            }
            accessValidator.checkAccess(path, Operation.DELETE);

            fileSystem.deleteIfExists(path);
            operationLogger.succeeded("DELETE_FILE", path, System.currentTimeMillis() - start);
            auditLogger.recordDeletion(path, actor);
        } catch (RuntimeException e) {
            operationLogger.failed("DELETE_FILE", path, e);
            throw e;
        }
    }

    /** Exclui um diretório vazio. Falha se não estiver vazio (use deleteDirectoryRecursively). */
    public void deleteEmptyDirectory(Path path) {
        deleteEmptyDirectory(path, "system");
    }

    public void deleteEmptyDirectory(Path path, String actor) {
        long start = System.currentTimeMillis();
        operationLogger.started("DELETE_EMPTY_DIR", path);
        try {
            if (!fileSystem.isDirectory(path)) {
                throw new OctopusFileException(ErrorCodes.DIRECTORY_NOT_FOUND, null, path);
            }
            if (!fileSystem.listDirectory(path).isEmpty()) {
                throw new OctopusFileException(ErrorCodes.INVALID_ARGUMENT, "Diretório não está vazio: " + path, path);
            }
            accessValidator.checkAccess(path, Operation.DELETE);

            fileSystem.deleteIfExists(path);
            operationLogger.succeeded("DELETE_EMPTY_DIR", path, System.currentTimeMillis() - start);
            auditLogger.recordDeletion(path, actor);
        } catch (RuntimeException e) {
            operationLogger.failed("DELETE_EMPTY_DIR", path, e);
            throw e;
        }
    }

    /** Exclui um diretório e todo o seu conteúdo recursivamente. Requer confirmação explícita. */
    public void deleteDirectoryRecursively(Path path, boolean recursive) {
        deleteDirectoryRecursively(path, recursive, "system");
    }

    public void deleteDirectoryRecursively(Path path, boolean recursive, String actor) {
        if (!recursive) {
            throw new OctopusFileException(ErrorCodes.INVALID_ARGUMENT,
                    "Exclusão recursiva requer recursive=true explícito"
            );
        }

        long start = System.currentTimeMillis();
        operationLogger.started("DELETE_DIR_RECURSIVE", path);
        try {
            if (!fileSystem.isDirectory(path)) {
                throw new OctopusFileException(ErrorCodes.DIRECTORY_NOT_FOUND, null, path);
            }
            accessValidator.checkAccess(path, Operation.DELETE);

            fileSystem.deleteRecursively(path);
            operationLogger.succeeded("DELETE_DIR_RECURSIVE", path, System.currentTimeMillis() - start);
            auditLogger.recordDeletion(path, actor);
        } catch (RuntimeException e) {
            operationLogger.failed("DELETE_DIR_RECURSIVE", path, e);
            throw e;
        }
    }

    /** Exclui se existir; não lança erro caso já não exista (idempotente). */
    public boolean deleteIfExists(Path path, String actor) {
        if (!fileSystem.exists(path)) {
            return false;
        }
        if (fileSystem.isDirectory(path)) {
            deleteDirectoryRecursively(path, true, actor);
        } else {
            deleteFile(path, actor);
        }
        return true;
    }
}
