package com.octopusfile.infrastructure.errors;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

/**
 * Centraliza a tradução de exceções de baixo nível (java.nio, java.io,
 * java.util.concurrent) para {@link OctopusFileException} com o
 * {@link ErrorCodes} apropriado. Todos os módulos devem passar por aqui
 * ao capturar exceções checked de I/O, garantindo mensagens e códigos
 * consistentes em toda a biblioteca.
 */
public final class ExceptionHandler {

    private ExceptionHandler() {
    }

    /**
     * Traduz uma exceção genérica capturada durante uma operação de arquivo
     * em uma {@link OctopusFileException} com o código de erro mais específico
     * possível.
     */
    public static OctopusFileException translate(Throwable ex, Path path) {
        if (ex instanceof OctopusFileException ofe) {
            return ofe;
        }
        if (ex instanceof NoSuchFileException) {
            return new OctopusFileException(ErrorCodes.FILE_NOT_FOUND, null, path, ex);
        }
        if (ex instanceof FileAlreadyExistsException) {
            return new OctopusFileException(ErrorCodes.FILE_ALREADY_EXISTS, null, path, ex);
        }
        if (ex instanceof AccessDeniedException) {
            return new OctopusFileException(ErrorCodes.ACCESS_DENIED, null, path, ex);
        }
        if (ex instanceof NotDirectoryException) {
            return new OctopusFileException(ErrorCodes.NOT_A_DIRECTORY, null, path, ex);
        }
        if (ex instanceof TimeoutException) {
            return new OctopusFileException(ErrorCodes.OPERATION_TIMEOUT, null, path, ex);
        }
        if (ex instanceof CancellationException) {
            return new OctopusFileException(ErrorCodes.OPERATION_CANCELLED, null, path, ex);
        }
        if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return new OctopusFileException(ErrorCodes.INTERRUPTED, null, path, ex);
        }
        if (ex instanceof java.io.IOException) {
            return new OctopusFileException(ErrorCodes.IO_ERROR, ex.getMessage(), path, ex);
        }
        return new OctopusFileException(ErrorCodes.UNKNOWN_ERROR, ex.getMessage(), path, ex);
    }

    public static OctopusFileException translate(Throwable ex) {
        return translate(ex, null);
    }

    /**
     * Executa uma operação que lança exceção checked, traduzindo qualquer
     * falha para OctopusFileException. Útil para encurtar blocos try/catch
     * repetitivos nos módulos de leitura/escrita/manipulação.
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static <T> T run(Path path, ThrowingSupplier<T> action) {
        try {
            return action.get();
        } catch (Exception ex) {
            throw translate(ex, path);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static void run(Path path, ThrowingRunnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            throw translate(ex, path);
        }
    }
}
