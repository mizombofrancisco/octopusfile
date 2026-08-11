package com.octopusfile.modules.reading;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.ExceptionHandler;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.validation.FileValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Leitura linha a linha com buffer, para arquivos grandes que não devem
 * ser carregados inteiramente na memória. Envolve {@code java.io.BufferedReader}
 * internamente (referenciado com nome totalmente qualificado para não
 * colidir com esta própria classe).
 *
 * Uso típico:
 * <pre>
 *   try (var reader = new BufferedReader(path)) {
 *       reader.forEachLine(line -> process(line));
 *   }
 * </pre>
 */
public class BufferedReader implements AutoCloseable {

    private final Path path;
    private final java.io.BufferedReader delegate;
    private boolean closed = false;

    public BufferedReader(Path path) {
        this(path, StandardCharsets.UTF_8, new NIO2FileSystem(), new FileValidator());
    }

    public BufferedReader(Path path, Charset charset, NIO2FileSystem fileSystem, FileValidator validator) {
        validator.validateExistingFile(path);
        this.path = path;
        InputStream in = fileSystem.newInputStream(path);
        this.delegate = new java.io.BufferedReader(new InputStreamReader(in, charset));
    }

    /** Lê e retorna a próxima linha, ou null ao chegar ao fim do arquivo. */
    public String readLine() {
        checkOpen();
        return ExceptionHandler.run(path, delegate::readLine);
    }

    /** Aplica {@code action} a cada linha do arquivo, na ordem, até o fim. */
    public void forEachLine(Consumer<String> action) {
        checkOpen();
        String line;
        while ((line = readLine()) != null) {
            action.accept(line);
        }
    }

    /**
     * Expõe as linhas como Stream preguiçoso. O stream DEVE ser fechado
     * (ou consumido dentro de try-with-resources) para liberar o arquivo
     * subjacente — fechar o stream fecha este BufferedReader.
     */
    public Stream<String> lines() {
        checkOpen();
        Iterator<String> iterator = new Iterator<>() {
            private String next;
            private boolean fetched = false;

            @Override
            public boolean hasNext() {
                if (!fetched) {
                    next = readLine();
                    fetched = true;
                }
                return next != null;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                fetched = false;
                return next;
            }
        };
        return StreamSupport.stream(
                java.util.Spliterators.spliteratorUnknownSize(iterator, java.util.Spliterator.ORDERED | java.util.Spliterator.NONNULL),
                false
        ).onClose(this::close);
    }

    private void checkOpen() {
        if (closed) {
            throw new OctopusFileException(ErrorCodes.STREAM_CLOSED, "Reader já foi fechado", path);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            delegate.close();
        } catch (IOException e) {
            throw ExceptionHandler.translate(e, path);
        }
    }
}
