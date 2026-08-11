package com.octopusfile.support.security;

import com.octopusfile.support.security.AccessValidator.Operation;
import com.octopusfile.support.validation.ExtensionValidator;
import com.octopusfile.support.validation.PathValidator;

import java.nio.file.Path;

/**
 * Fachada de segurança usada pelos módulos de alto nível: combina
 * validação de caminho (traversal), extensão e permissão em uma única
 * chamada de "gate" antes de qualquer operação de I/O.
 */
public class SecureFileHandler {

    private final PathValidator pathValidator;
    private final ExtensionValidator extensionValidator;
    private final AccessValidator accessValidator;
    private final Path sandboxRoot; // null = sem sandbox, qualquer caminho absoluto é aceito

    public SecureFileHandler(PathValidator pathValidator,
                              ExtensionValidator extensionValidator,
                              AccessValidator accessValidator,
                              Path sandboxRoot) {
        this.pathValidator = pathValidator;
        this.extensionValidator = extensionValidator;
        this.accessValidator = accessValidator;
        this.sandboxRoot = sandboxRoot;
    }

    public SecureFileHandler() {
        this(new PathValidator(), new ExtensionValidator(), new AccessValidator(), null);
    }

    /** Cria um handler restrito a um diretório raiz (sandbox), útil para uploads de usuário final. */
    public static SecureFileHandler sandboxedTo(Path root) {
        return new SecureFileHandler(new PathValidator(), new ExtensionValidator(), new AccessValidator(), root);
    }

    /**
     * Executa todas as checagens de segurança para uma operação sobre {@code path}.
     * Lança OctopusFileException no primeiro problema encontrado.
     */
    public Path authorize(Path path, Operation operation) {
        Path resolved = path;
        if (sandboxRoot != null) {
            resolved = pathValidator.ensureWithinRoot(sandboxRoot, path);
        } else if (pathValidator.containsTraversal(path)) {
            // fora de sandbox, ainda vale a pena avisar sobre ".." suspeito
            resolved = path.normalize();
        }

        if (operation == Operation.WRITE) {
            extensionValidator.validate(resolved);
        }

        accessValidator.checkAccess(resolved, operation);
        return resolved;
    }

    public AccessValidator accessValidator() {
        return accessValidator;
    }

    public PathValidator pathValidator() {
        return pathValidator;
    }

    public ExtensionValidator extensionValidator() {
        return extensionValidator;
    }
}
