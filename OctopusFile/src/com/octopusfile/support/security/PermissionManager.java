package com.octopusfile.support.security;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.ExceptionHandler;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Gerencia permissões POSIX de arquivos (leitura/escrita/execução por
 * dono/grupo/outros). Em sistemas que não suportam POSIX (ex.: Windows),
 * os métodos de leitura de permissão retornam um conjunto vazio e os
 * de escrita lançam OctopusFileException com PERMISSION_DENIED, já que
 * a operação não tem equivalente direto.
 */
public class PermissionManager {

    public boolean supportsPosix(Path path) {
        return path.getFileSystem().supportedFileAttributeViews().contains("posix");
    }

    public Set<PosixFilePermission> getPermissions(Path path) {
        if (!supportsPosix(path)) {
            return Set.of();
        }
        return ExceptionHandler.run(path, () -> Files.getPosixFilePermissions(path));
    }

    public void setPermissions(Path path, Set<PosixFilePermission> permissions) {
        if (!supportsPosix(path)) {
            throw new OctopusFileException(
                    ErrorCodes.PERMISSION_DENIED,
                    "Sistema de arquivos não suporta permissões POSIX",
                    path
            );
        }
        ExceptionHandler.run(path, () -> Files.setPosixFilePermissions(path, permissions));
    }

    /** Define permissões a partir de uma string estilo "rwxr-xr--". */
    public void setPermissions(Path path, String posixString) {
        setPermissions(path, PosixFilePermissions.fromString(posixString));
    }

    public boolean isReadable(Path path) {
        return Files.isReadable(path);
    }

    public boolean isWritable(Path path) {
        return Files.isWritable(path);
    }

    public boolean isExecutable(Path path) {
        return Files.isExecutable(path);
    }

    /** Aplica permissões restritivas típicas de arquivo sensível: dono rw, sem acesso para grupo/outros. */
    public void makePrivate(Path path) {
        setPermissions(path, "rw-------");
    }

    /** Aplica permissões de somente leitura para todos. */
    public void makeReadOnly(Path path) {
        setPermissions(path, "r--r--r--");
    }
}
