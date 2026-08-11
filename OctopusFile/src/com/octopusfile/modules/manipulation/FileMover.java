package com.octopusfile.modules.manipulation;

import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.logging.OperationLogger;
import com.octopusfile.support.security.AccessValidator;
import com.octopusfile.support.security.AccessValidator.Operation;
import com.octopusfile.support.validation.FileValidator;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Move (renomeia entre diretórios) arquivos e diretórios. Diferente de
 * FileCopier, uma movimentação é uma operação destrutiva na origem — por
 * isso exige checagem de WRITE (não apenas READ) tanto na origem quanto
 * no destino, já que a origem deixa de existir.
 */
public class FileMover {

    private final NIO2FileSystem fileSystem;
    private final FileValidator validator;
    private final AccessValidator accessValidator;
    private final OperationLogger logger;

    public FileMover() {
        this(new NIO2FileSystem(), new FileValidator(), new AccessValidator(), new OperationLogger());
    }

    public FileMover(NIO2FileSystem fileSystem, FileValidator validator, AccessValidator accessValidator, OperationLogger logger) {
        this.fileSystem = fileSystem;
        this.validator = validator;
        this.accessValidator = accessValidator;
        this.logger = logger;
    }

    public Path move(Path source, Path target) {
        return move(source, target, false);
    }

    public Path move(Path source, Path target, boolean overwrite) {
        long start = System.currentTimeMillis();
        logger.started("MOVE", source);
        try {
            if (!fileSystem.exists(source)) {
                validator.validateExistingFile(source); // lança FILE_NOT_FOUND com mensagem consistente
            }
            accessValidator.checkAccess(source, Operation.WRITE);
            if (!overwrite) {
                validator.validateDoesNotExist(target);
            }

            Path result = overwrite
                    ? fileSystem.move(source, target, StandardCopyOption.REPLACE_EXISTING)
                    : fileSystem.move(source, target);

            logger.succeeded("MOVE", source, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException e) {
            logger.failed("MOVE", source, e);
            throw e;
        }
    }

    /**
     * Move preservando atomicidade quando o sistema de arquivos permite
     * (mesma partição/volume). Se o destino estiver em outro volume,
     * lança a mesma exceção que java.nio seria: use {@link #move} normal
     * como fallback nesse caso.
     */
    public Path moveAtomic(Path source, Path target) {
        long start = System.currentTimeMillis();
        logger.started("MOVE_ATOMIC", source);
        try {
            validator.validateExistingFile(source);
            accessValidator.checkAccess(source, Operation.WRITE);
            validator.validateDoesNotExist(target);

            Path result = fileSystem.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            logger.succeeded("MOVE_ATOMIC", source, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException e) {
            logger.failed("MOVE_ATOMIC", source, e);
            throw e;
        }
    }
}
