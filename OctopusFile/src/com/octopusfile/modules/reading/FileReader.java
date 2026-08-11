package com.octopusfile.modules.reading;

import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.cache.ContentCache;
import com.octopusfile.support.validation.FileValidator;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Leitura simples "tudo de uma vez" — carrega o arquivo inteiro em memória
 * como String ou bytes. Adequado para arquivos pequenos/médios (config,
 * templates, JSON). Para arquivos grandes, prefira {@link BufferedReader}
 * (linha a linha) ou {@link BinaryReader} (chunks), que não carregam tudo
 * de uma vez na heap.
 */
public class FileReader {

    private final NIO2FileSystem fileSystem;
    private final FileValidator validator;
    private final ContentCache contentCache; // pode ser null (sem cache)

    public FileReader() {
        this(new NIO2FileSystem(), new FileValidator(), null);
    }

    public FileReader(NIO2FileSystem fileSystem, FileValidator validator, ContentCache contentCache) {
        this.fileSystem = fileSystem;
        this.validator = validator;
        this.contentCache = contentCache;
    }

    public String readText(Path path) {
        return readText(path, StandardCharsets.UTF_8);
    }

    public String readText(Path path, Charset charset) {
        validator.validateExistingFile(path);
        byte[] bytes = readBytes(path);
        return new String(bytes, charset);
    }


    public String readAllText(Path path) throws IOException {
        return Files.readString(path);
    }


    public byte[] readBytes(Path path) {
        validator.validateExistingFile(path);
        if (contentCache != null) {
            return contentCache.getOrLoad(path, fileSystem::readAllBytes);
        }
        return fileSystem.readAllBytes(path);
    }

    /** Lê o arquivo como texto e o divide em linhas (equivalente a readAllLines, mas por readText). */
    public java.util.List<String> readLinesEager(Path path) {
        validator.validateExistingFile(path);
        return fileSystem.readAllLines(path);
    }

    public long sizeOf(Path path) {
        validator.validateExistingFile(path);
        return fileSystem.size(path);
    }

    public boolean exists(Path path) {
        return fileSystem.exists(path);
    }
}
