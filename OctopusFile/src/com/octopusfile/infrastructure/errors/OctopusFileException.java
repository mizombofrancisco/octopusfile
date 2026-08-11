package com.octopusfile.infrastructure.errors;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Exceção raiz de toda a biblioteca OctopusFile.
 * Carrega um {@link ErrorCodes} para permitir tratamento programático
 * (switch/case, telemetria, mapeamento para respostas de API, etc.)
 * além da mensagem legível para humanos.
 */
public class OctopusFileException extends RuntimeException {

    private final ErrorCodes errorCode;
    private final Path path; // pode ser null quando não se aplica

    public OctopusFileException(ErrorCodes errorCode, Object cause) {
        this(errorCode, errorCode.defaultMessage(), null, null);
    }

    public OctopusFileException(ErrorCodes errorCode, IOException message) {
        this(errorCode, message.getMessage(), null, null);
    }

    public OctopusFileException(ErrorCodes errorCode, String message, Path path) {
        this(errorCode, message, path, null);
    }

    public OctopusFileException(ErrorCodes errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    public OctopusFileException(ErrorCodes errorCode, String message, Path path, Throwable cause) {
        super(buildMessage(errorCode, message, path), cause);
        this.errorCode = errorCode;
        this.path = path;
    }

    private static String buildMessage(ErrorCodes errorCode, String message, Path path) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(errorCode.code()).append("] ");
        sb.append(message != null ? message : errorCode.defaultMessage());
        if (path != null) {
            sb.append(" (caminho: ").append(path).append(')');
        }
        return sb.toString();
    }

    public ErrorCodes errorCode() {
        return errorCode;
    }

    public Path path() {
        return path;
    }
}
