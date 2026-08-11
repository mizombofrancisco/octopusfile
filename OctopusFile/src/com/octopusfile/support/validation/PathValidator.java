package com.octopusfile.support.validation;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Valida caminhos antes que cheguem à camada de sistema de arquivos.
 * Foca em dois tipos de problema:
 *  1) sintaxe inválida (caracteres proibidos, caminho vazio);
 *  2) path traversal (".." saindo de uma raiz permitida) — importante
 *     quando caminhos vêm de entrada não confiável (ex.: nome de arquivo
 *     enviado por um usuário final de uma aplicação que usa a lib).
 */
public class PathValidator {

    /** Converte uma string em Path, lançando OctopusFileException em vez de InvalidPathException. */
    public Path parse(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new OctopusFileException(ErrorCodes.INVALID_PATH, "Caminho não pode ser vazio ou nulo");
        }
        try {
            return Paths.get(rawPath);
        } catch (InvalidPathException e) {
            throw new OctopusFileException(ErrorCodes.INVALID_PATH, "Caminho malformado: " + rawPath, e);
        }
    }

    /**
     * Garante que {@code target}, depois de normalizado, permanece dentro de {@code root}.
     * Lança SECURITY_VIOLATION se houver tentativa de escapar da raiz (ex.: "../../etc/passwd").
     */
    public Path ensureWithinRoot(Path root, Path target) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedTarget = normalizedRoot.resolve(target).normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new OctopusFileException(
                    ErrorCodes.SECURITY_VIOLATION,
                    "Caminho tenta escapar do diretório raiz permitido",
                    normalizedTarget
            );
        }
        return normalizedTarget;
    }

    public boolean isValidSyntax(String rawPath) {
        try {
            parse(rawPath);
            return true;
        } catch (OctopusFileException e) {
            return false;
        }
    }

    public boolean isAbsolute(Path path) {
        return path.isAbsolute();
    }

    public boolean containsTraversal(Path path) {
        for (Path part : path) {
            if (part.toString().equals("..")) {
                return true;
            }
        }
        return false;
    }
}
