package com.octopusfile.modules.writing;

import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.validation.FileValidator;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Escrita simples "tudo de uma vez": grava um conteúdo completo (String ou
 * bytes) em um arquivo, criando diretórios pai automaticamente. Para
 * escrita incremental linha a linha, veja {@link BufferedWriter}; para
 * grandes volumes binários em streaming, veja {@link BinaryWriter}.
 */
public class FileWriter {

    private final NIO2FileSystem fileSystem;
    private final FileValidator validator;

    public FileWriter() {
        this(new NIO2FileSystem(), new FileValidator());
    }

    public FileWriter(NIO2FileSystem fileSystem, FileValidator validator) {
        this.fileSystem = fileSystem;
        this.validator = validator;
    }

    /** Escreve texto, sobrescrevendo o arquivo se já existir (cria se não existir). */
    public Path writeText(Path path, String content) {
        return writeText(path, content, StandardCharsets.UTF_8);
    }

    public Path writeText(Path path, String content, Charset charset) {
        validator.validateForWrite(path);
        return fileSystem.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Acrescenta texto ao final do arquivo, criando-o se ainda não existir. */
    public Path appendText(Path path, String content) {
        return appendText(path, content, StandardCharsets.UTF_8);
    }

    public Path appendText(Path path, String content, Charset charset) {
        validator.validateForWrite(path);
        return fileSystem.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public Path writeBytes(Path path, byte[] content) {
        validator.validateForWrite(path);
        return fileSystem.writeBytes(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Path appendBytes(Path path, byte[] content) {
        validator.validateForWrite(path);
        return fileSystem.writeBytes(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Cria um arquivo vazio, falhando se já existir. */
    public Path createNew(Path path) {
        validator.validateForWrite(path);
        validator.validateDoesNotExist(path);
        return fileSystem.writeBytes(path, new byte[0], StandardOpenOption.CREATE_NEW);
    }
}
