package com.octopusfile.modules.manipulation;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.logging.OperationLogger;
import com.octopusfile.support.security.AccessValidator;
import com.octopusfile.support.security.AccessValidator.Operation;
import com.octopusfile.support.validation.FileValidator;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Renomeia arquivos e diretórios dentro do mesmo diretório pai. Difere de
 * FileMover por validar explicitamente que o novo nome é um nome de
 * arquivo válido (sem separadores de caminho, sem caracteres reservados
 * do SO) — renomear não deve permitir mudar de diretório "por acidente"
 * via um nome como "../outro/arquivo.txt".
 */
public class FileRenamer {

    // Caracteres reservados em Windows; interseção segura para portabilidade.
    private static final Pattern INVALID_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    private final NIO2FileSystem fileSystem;
    private final FileValidator validator;
    private final AccessValidator accessValidator;
    private final OperationLogger logger;

    public FileRenamer() {
        this(new NIO2FileSystem(), new FileValidator(), new AccessValidator(), new OperationLogger());
    }

    public FileRenamer(NIO2FileSystem fileSystem, FileValidator validator, AccessValidator accessValidator, OperationLogger logger) {
        this.fileSystem = fileSystem;
        this.validator = validator;
        this.accessValidator = accessValidator;
        this.logger = logger;
    }

    public Path rename(Path path, String newName) {
        return rename(path, newName, false);
    }

    public Path rename(Path path, String newName, boolean overwrite) {
        validateSimpleName(newName);

        long start = System.currentTimeMillis();
        logger.started("RENAME", path);
        try {
            if (!fileSystem.exists(path)) {
                throw new OctopusFileException(ErrorCodes.PATH_NOT_FOUND, null, path);
            }
            accessValidator.checkAccess(path, Operation.WRITE);

            Path target = path.resolveSibling(newName);
            if (!overwrite) {
                validator.validateDoesNotExist(target);
            }

            Path result = overwrite
                    ? fileSystem.move(path, target, StandardCopyOption.REPLACE_EXISTING)
                    : fileSystem.move(path, target);

            logger.succeeded("RENAME", path, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException e) {
            logger.failed("RENAME", path, e);
            throw e;
        }
    }

    /** Valida que {@code name} é um nome de arquivo simples, sem componentes de diretório. */
    private void validateSimpleName(String name) {
        if (name == null || name.isBlank()) {
            throw new OctopusFileException(ErrorCodes.INVALID_ARGUMENT, "Novo nome não pode ser vazio");
        }
        if (name.equals(".") || name.equals("..")) {
            throw new OctopusFileException(ErrorCodes.INVALID_ARGUMENT, "Novo nome não pode ser '.' ou '..'");
        }
        if (INVALID_NAME_CHARS.matcher(name).find()) {
            throw new OctopusFileException(ErrorCodes.INVALID_ARGUMENT, "Novo nome contém caracteres não permitidos: " + name);
        }
    }
}
