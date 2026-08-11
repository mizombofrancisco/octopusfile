package com.octopusfile.modules.writing;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.ExceptionHandler;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.validation.FileValidator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Escrita binária em streaming (chunk a chunk), para gravação de grandes
 * volumes de dados binários (uploads, downloads, transcodificação) sem
 * acumular tudo em memória antes de gravar — complemento de
 * {@link com.octopusfile.modules.reading.BinaryReader} no fluxo de cópia
 * manual entre streams.
 */
public class BinaryWriter implements AutoCloseable {

    private final Path path;
    private final OutputStream outputStream;
    private long bytesWritten = 0;
    private boolean closed = false;

    public BinaryWriter(Path path) {
        this(path, false, new NIO2FileSystem(), new FileValidator());
    }

    public BinaryWriter(Path path, boolean append) {
        this(path, append, new NIO2FileSystem(), new FileValidator());
    }

    public BinaryWriter(Path path, boolean append, NIO2FileSystem fileSystem, FileValidator validator) {
        validator.validateForWrite(path);
        this.path = path;
        StandardOpenOption[] options = append
                ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        this.outputStream = fileSystem.newOutputStream(path, options);
    }

    public void writeChunk(byte[] chunk) {
        writeChunk(chunk, 0, chunk.length);
    }

    public void writeChunk(byte[] chunk, int offset, int length) {
        checkOpen();
        ExceptionHandler.run(path, () -> outputStream.write(chunk, offset, length));
        bytesWritten += length;
    }

    /** Copia todo o conteúdo de um InputStream para este writer, em blocos, sem carregar tudo em memória. */
    public long writeFrom(java.io.InputStream source, int chunkSize) {
        checkOpen();
        byte[] buffer = new byte[chunkSize];
        long total = 0;
        int read;
        try {
            while ((read = source.read(buffer)) != -1) {
                writeChunk(buffer, 0, read);
                total += read;
            }
        } catch (IOException e) {
            throw ExceptionHandler.translate(e, path);
        }
        return total;
    }

    public long writeFrom(java.io.InputStream source) {
        return writeFrom(source, 8192);
    }

    public void flush() {
        checkOpen();
        ExceptionHandler.run(path, outputStream::flush);
    }

    public long bytesWritten() {
        return bytesWritten;
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
            outputStream.close();
        } catch (IOException e) {
            throw ExceptionHandler.translate(e, path);
        }
    }
}
