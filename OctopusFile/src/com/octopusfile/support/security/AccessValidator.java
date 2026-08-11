package com.octopusfile.support.security;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.nio.file.Path;

/**
 * Verifica se uma operação (ler, escrever, excluir) pode ser realizada
 * sobre um caminho, combinando permissões do SO (via PermissionManager)
 * com regras adicionais de negócio (ex.: diretórios protegidos que a
 * aplicação decide nunca deixar excluir, mesmo que o SO permita).
 */
public class AccessValidator {

    public enum Operation { READ, WRITE, DELETE, EXECUTE }

    private final PermissionManager permissionManager;
    private final java.util.Set<Path> protectedPaths;

    public AccessValidator(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
        this.protectedPaths = java.util.concurrent.ConcurrentHashMap.newKeySet();
    }

    public AccessValidator() {
        this(new PermissionManager());
    }

    /** Marca um caminho (arquivo ou diretório) como protegido contra escrita/exclusão. */
    public void protect(Path path) {
        protectedPaths.add(path.toAbsolutePath().normalize());
    }

    public void unprotect(Path path) {
        protectedPaths.remove(path.toAbsolutePath().normalize());
    }

    public boolean isProtected(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        for (Path protectedPath : protectedPaths) {
            if (normalized.equals(protectedPath) || normalized.startsWith(protectedPath)) {
                return true;
            }
        }
        return false;
    }

    /** Lança OctopusFileException se a operação não for permitida; caso contrário retorna normalmente. */
    public void checkAccess(Path path, Operation operation) {
        if (operation != Operation.READ && isProtected(path)) {
            throw new OctopusFileException(
                    ErrorCodes.SECURITY_VIOLATION,
                    "Caminho está protegido contra operações de " + operation,
                    path
            );
        }

        boolean allowed = switch (operation) {
            case READ -> permissionManager.isReadable(path);
            case WRITE, DELETE -> permissionManager.isWritable(path);
            case EXECUTE -> permissionManager.isExecutable(path);
        };

        if (!allowed) {
            throw new OctopusFileException(
                    ErrorCodes.PERMISSION_DENIED,
                    "Permissão negada para operação " + operation,
                    path
            );
        }
    }

    public boolean hasAccess(Path path, Operation operation) {
        try {
            checkAccess(path, operation);
            return true;
        } catch (OctopusFileException e) {
            return false;
        }
    }
}
