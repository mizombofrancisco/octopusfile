package com.octopusfile.modules.manipulation;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.ExceptionHandler;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.logging.OperationLogger;
import com.octopusfile.support.security.AccessValidator;
import com.octopusfile.support.security.AccessValidator.Operation;
import com.octopusfile.support.validation.FileValidator;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Copia arquivos e árvores de diretório. Toda cópia passa por
 * AccessValidator (leitura na origem, escrita no destino) antes de tocar
 * o disco, e é logada via OperationLogger para diagnóstico.
 */
public class FileCopier {

    private final NIO2FileSystem fileSystem;
    private final FileValidator validator;
    private final AccessValidator accessValidator;
    private final OperationLogger logger;

    public FileCopier() {
        this(new NIO2FileSystem(), new FileValidator(), new AccessValidator(), new OperationLogger());
    }

    public FileCopier(NIO2FileSystem fileSystem, FileValidator validator, AccessValidator accessValidator, OperationLogger logger) {
        this.fileSystem = fileSystem;
        this.validator = validator;
        this.accessValidator = accessValidator;
        this.logger = logger;
    }

    /** Copia um único arquivo. Por padrão não sobrescreve destino existente. */
    public Path copyFile(Path source, Path target) {
        return copyFile(source, target, false);
    }

    public Path copyFile(Path source, Path target, boolean overwrite) {
        long start = System.currentTimeMillis();
        logger.started("COPY_FILE", source);
        try {
            validator.validateExistingFile(source);
            accessValidator.checkAccess(source, Operation.READ);
            if (!overwrite) {
                validator.validateDoesNotExist(target);
            }

            Path result = overwrite
                    ? fileSystem.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                    : fileSystem.copy(source, target);

            logger.succeeded("COPY_FILE", source, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException e) {
            logger.failed("COPY_FILE", source, e);
            throw e;
        }
    }

    /**
     * Copia recursivamente um diretório inteiro para um novo destino,
     * preservando a estrutura relativa. Falha rapidamente (não continua
     * parcialmente) se qualquer arquivo individual falhar — para cópia
     * tolerante a falhas parciais, use OperacoesEmLote.
     */
    public Path copyDirectory(Path sourceDir, Path targetDir) {
        long start = System.currentTimeMillis();
        logger.started("COPY_DIRECTORY", sourceDir);
        try {
            validator.validateExistingDirectory(sourceDir);
            accessValidator.checkAccess(sourceDir, Operation.READ);

            List<Path> allEntries = fileSystem.walk(sourceDir);
            for (Path entry : allEntries) {
                Path relative = sourceDir.relativize(entry);
                Path destination = targetDir.resolve(relative);
                if (fileSystem.isDirectory(entry)) {
                    fileSystem.createDirectories(destination);
                } else {
                    fileSystem.copy(entry, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            logger.succeeded("COPY_DIRECTORY", sourceDir, System.currentTimeMillis() - start);
            return targetDir;
        } catch (RuntimeException e) {
            logger.failed("COPY_DIRECTORY", sourceDir, e);
            throw e;
        }
    }

    /** Copia genérica: detecta se a origem é arquivo ou diretório e delega adequadamente. */
    public Path copy(Path source, Path target) {
        if (fileSystem.isDirectory(source)) {
            return copyDirectory(source, target);
        }
        if (fileSystem.isRegularFile(source)) {
            return copyFile(source, target);
        }
        throw new OctopusFileException(ErrorCodes.PATH_NOT_FOUND, "Origem não é arquivo nem diretório", source);
    }
}
