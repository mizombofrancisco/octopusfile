package com.octopusfile.modules.reading;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.ExceptionHandler;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.validation.FileValidator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Leitura binária em blocos (chunks) de tamanho fixo, para arquivos grandes
 * ou de conteúdo não-textual (imagens, binários, arquivos compactados)
 * onde carregar tudo em memória (FileReader) não é viável.
 */
public class BinaryReader implements AutoCloseable {

    private static final int DEFAULT_CHUNK_SIZE = 8192;

    private final Path path;
    private final InputStream inputStream;
    private boolean closed = false;

    public BinaryReader(Path path) {
        this(path, new NIO2FileSystem(), new FileValidator());
    }

    public BinaryReader(Path path, NIO2FileSystem fileSystem, FileValidator validator) {
        validator.validateExistingFile(path);
        this.path = path;
        this.inputStream = fileSystem.newInputStream(path);
    }

    /** Lê até {@code chunkSize} bytes; retorna array menor no fim do arquivo, ou null se já no EOF. */
    public byte[] readChunk(int chunkSize) {
        checkOpen();
        byte[] buffer = new byte[chunkSize];
        int read = ExceptionHandler.run(path, () -> inputStream.read(buffer));
        if (read == -1) {
            return null;
        }
        if (read == chunkSize) {
            return buffer;
        }
        byte[] trimmed = new byte[read];
        System.arraycopy(buffer, 0, trimmed, 0, read);
        return trimmed;
    }

    public byte[] readChunk() {
        return readChunk(DEFAULT_CHUNK_SIZE);
    }

    /** Processa o arquivo inteiro em blocos, chamando {@code consumer} para cada um, sem reter tudo em memória. */
    public void forEachChunk(int chunkSize, Consumer<byte[]> consumer) {
        byte[] chunk;
        while ((chunk = readChunk(chunkSize)) != null) {
            consumer.accept(chunk);
        }
    }

    public void forEachChunk(Consumer<byte[]> consumer) {
        forEachChunk(DEFAULT_CHUNK_SIZE, consumer);
    }

    /** Pula {@code n} bytes à frente na leitura. */
    public long skip(long n) {
        checkOpen();
        return ExceptionHandler.run(path, () -> inputStream.skip(n));
    }

    public InputStream asInputStream() {
        checkOpen();
        return inputStream;
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
            inputStream.close();
        } catch (IOException e) {
            throw ExceptionHandler.translate(e, path);
        }
    }
}
