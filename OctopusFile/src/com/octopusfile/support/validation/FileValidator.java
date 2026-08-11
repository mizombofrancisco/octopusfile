package com.octopusfile.support.validation;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;

import java.nio.file.Path;

/**
 * Ponto único de validação usado pelos módulos de leitura/escrita/manipulação
 * antes de tocar o disco. Combina PathValidator + ExtensionValidator com
 * checagens de existência/tipo via NIO2FileSystem.
 */
public class FileValidator {

    private final PathValidator pathValidator;
    private final ExtensionValidator extensionValidator;
    private final NIO2FileSystem fileSystem;

    public FileValidator(PathValidator pathValidator, ExtensionValidator extensionValidator, NIO2FileSystem fileSystem) {
        this.pathValidator = pathValidator;
        this.extensionValidator = extensionValidator;
        this.fileSystem = fileSystem;
    }

    public FileValidator() {
        this(new PathValidator(), new ExtensionValidator(), new NIO2FileSystem());
    }

    /** Valida que o caminho é sintaticamente correto e a extensão é permitida (não checa existência). */
    public void validateForWrite(Path path) {
        if (path == null) {
            throw new OctopusFileException(ErrorCodes.INVALID_PATH, "Caminho não pode ser nulo");
        }
        extensionValidator.validate(path);
    }

    /** Valida que o arquivo existe e é de fato um arquivo regular (não diretório). */
    public void validateExistingFile(Path path) {
        if (!fileSystem.exists(path)) {
            throw new OctopusFileException(ErrorCodes.FILE_NOT_FOUND, null, path);
        }
        if (!fileSystem.isRegularFile(path)) {
            throw new OctopusFileException(ErrorCodes.NOT_A_FILE, null, path);
        }
    }

    /** Valida que o diretório existe e é de fato um diretório. */
    public void validateExistingDirectory(Path path) {
        if (!fileSystem.exists(path)) {
            throw new OctopusFileException(ErrorCodes.DIRECTORY_NOT_FOUND, null, path);
        }
        if (!fileSystem.isDirectory(path)) {
            throw new OctopusFileException(ErrorCodes.NOT_A_DIRECTORY, null, path);
        }
    }

    /** Garante que não há um arquivo já ocupando o destino (para operações que não sobrescrevem). */
    public void validateDoesNotExist(Path path) {
        if (fileSystem.exists(path)) {
            throw new OctopusFileException(ErrorCodes.FILE_ALREADY_EXISTS, null, path);
        }
    }

    public void validateWithinRoot(Path root, Path target) {
        pathValidator.ensureWithinRoot(root, target);
    }

    public PathValidator paths() {
        return pathValidator;
    }

    public ExtensionValidator extensions() {
        return extensionValidator;
    }
}
