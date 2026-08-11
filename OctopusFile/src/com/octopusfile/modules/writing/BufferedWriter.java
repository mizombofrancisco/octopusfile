package com.octopusfile.modules.writing;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.ExceptionHandler;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.validation.FileValidator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Escrita em buffer, linha a linha, para geração incremental de arquivos
 * grandes (relatórios, exports) sem manter tudo em memória antes de gravar.
 * Envolve {@code java.io.BufferedWriter} internamente (nome totalmente
 * qualificado para não colidir com esta classe).
 */
public class BufferedWriter implements AutoCloseable {

    private final Path path;
    private final java.io.BufferedWriter delegate;
    private boolean closed = false;

    public BufferedWriter(Path path) {
        this(path, false, StandardCharsets.UTF_8, new NIO2FileSystem(), new FileValidator());
    }

    public BufferedWriter(Path path, boolean append) {
        this(path, append, StandardCharsets.UTF_8, new NIO2FileSystem(), new FileValidator());
    }

    public BufferedWriter(Path path, boolean append, Charset charset, NIO2FileSystem fileSystem, FileValidator validator) {
        validator.validateForWrite(path);
        this.path = path;
        StandardOpenOption[] options = append
                ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        OutputStream out = fileSystem.newOutputStream(path, options);
        this.delegate = new java.io.BufferedWriter(new OutputStreamWriter(out, charset));
    }

    public void writeLine(String line) {
        checkOpen();
        ExceptionHandler.run(path, () -> {
            delegate.write(line);
            delegate.newLine();
        });
    }

    public void writeLines(List<String> lines) {
        for (String line : lines) {
            writeLine(line);
        }
    }

    public void write(String text) {
        checkOpen();
        ExceptionHandler.run(path, () -> delegate.write(text));
    }

    /** Força a gravação do buffer em disco sem fechar o writer. */
    public void flush() {
        checkOpen();
        ExceptionHandler.run(path, delegate::flush);
    }

    private void checkOpen() {
        if (closed) {
            throw new OctopusFileException(ErrorCodes.STREAM_CLOSED, "Writer já foi fechado", path);
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
