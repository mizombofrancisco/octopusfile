package com.octopusfile;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ponto de acesso simplificado para operações comuns sobre diretórios.
 */
public class DirectoryManager {

    /**
     * Cria um diretório e todos os diretórios pais necessários.
     *
     * @param path caminho do diretório
     * @throws IOException caso o diretório não possa ser criado
     */
    public void createDirectory(Path path) throws IOException {
        validatePath(path);

        Files.createDirectories(path);
    }

    /**
     * Exclui um diretório vazio caso ele exista.
     *
     * @param path caminho do diretório
     * @throws IOException caso o diretório não possa ser excluído
     */
    public void deleteDirectory(Path path) throws IOException {
        validatePath(path);

        Files.deleteIfExists(path);
    }

    /**
     * Exclui um diretório juntamente com todo o seu conteúdo.
     *
     * @param path caminho do diretório
     * @throws IOException caso algum item não possa ser excluído
     */
    public void deleteDirectoryRecursively(Path path) throws IOException {
        validatePath(path);

        if (!Files.exists(path)) {
            return;
        }

        Files.walkFileTree(
                path,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs
                    ) throws IOException {

                        Files.delete(file);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(
                            Path directory,
                            IOException exception
                    ) throws IOException {

                        if (exception != null) {
                            throw exception;
                        }

                        Files.delete(directory);

                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

    /**
     * Verifica se um caminho corresponde a um diretório.
     *
     * @param path caminho a verificar
     * @return true se for um diretório
     */
    public boolean directoryExists(Path path) {
        return path != null && Files.isDirectory(path);
    }

    /**
     * Verifica se o diretório está vazio.
     *
     * @param path caminho do diretório
     * @return true se não possuir arquivos ou subdiretórios
     * @throws IOException caso o diretório não possa ser lido
     */
    public boolean isEmpty(Path path) throws IOException {
        validateDirectory(path);

        try (Stream<Path> stream = Files.list(path)) {
            return stream.findFirst().isEmpty();
        }
    }

    /**
     * Lista os arquivos e diretórios diretamente dentro do diretório.
     *
     * @param path caminho do diretório
     * @return lista de itens encontrados
     * @throws IOException caso o diretório não possa ser lido
     */
    public List<Path> list(Path path) throws IOException {
        validateDirectory(path);

        try (Stream<Path> stream = Files.list(path)) {
            return stream.toList();
        }
    }

    /**
     * Lista recursivamente todos os arquivos e diretórios.
     *
     * @param path caminho do diretório
     * @return lista de itens encontrados
     * @throws IOException caso o diretório não possa ser lido
     */
    public List<Path> listRecursively(Path path) throws IOException {
        validateDirectory(path);

        try (Stream<Path> stream = Files.walk(path)) {
            return stream.toList();
        }
    }

    /**
     * Retorna somente os arquivos existentes diretamente no diretório.
     *
     * @param path caminho do diretório
     * @return lista de arquivos
     * @throws IOException caso o diretório não possa ser lido
     */
    public List<Path> listFiles(Path path) throws IOException {
        validateDirectory(path);

        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .toList();
        }
    }

    /**
     * Retorna somente os subdiretórios existentes diretamente no diretório.
     *
     * @param path caminho do diretório
     * @return lista de subdiretórios
     * @throws IOException caso o diretório não possa ser lido
     */
    public List<Path> listDirectories(Path path) throws IOException {
        validateDirectory(path);

        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(Files::isDirectory)
                    .toList();
        }
    }

    /**
     * Conta os itens diretamente existentes no diretório.
     *
     * @param path caminho do diretório
     * @return quantidade de itens
     * @throws IOException caso o diretório não possa ser lido
     */
    public long count(Path path) throws IOException {
        validateDirectory(path);

        try (Stream<Path> stream = Files.list(path)) {
            return stream.count();
        }
    }

    /**
     * Copia um diretório e todo o seu conteúdo.
     *
     * @param source diretório de origem
     * @param target diretório de destino
     * @throws IOException caso algum item não possa ser copiado
     */
    public void copyDirectory(
            Path source,
            Path target
    ) throws IOException {

        validateDirectory(source);
        validatePath(target);

        Files.walkFileTree(
                source,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory,
                            BasicFileAttributes attrs
                    ) throws IOException {

                        Path targetDirectory =
                                target.resolve(
                                        source.relativize(directory)
                                );

                        Files.createDirectories(targetDirectory);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs
                    ) throws IOException {

                        Path targetFile =
                                target.resolve(
                                        source.relativize(file)
                                );

                        Files.copy(
                                file,
                                targetFile,
                                StandardCopyOption.REPLACE_EXISTING
                        );

                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

    /**
     * Move um diretório para outro local.
     *
     * @param source diretório de origem
     * @param target diretório de destino
     * @throws IOException caso o diretório não possa ser movido
     */
    public void moveDirectory(
            Path source,
            Path target
    ) throws IOException {

        validateDirectory(source);
        validatePath(target);

        Path parent = target.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    /**
     * Obtém o tamanho total de um diretório e seu conteúdo em bytes.
     *
     * @param path caminho do diretório
     * @return tamanho total em bytes
     * @throws IOException caso o conteúdo não possa ser lido
     */
    public long size(Path path) throws IOException {
        validateDirectory(path);

        try (Stream<Path> stream = Files.walk(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            return Files.size(file);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        }
    }

    /**
     * Valida se o caminho não é nulo.
     */
    private void validatePath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException(
                    "O caminho não pode ser nulo"
            );
        }
    }

    /**
     * Valida se o caminho corresponde a um diretório existente.
     */
    private void validateDirectory(Path path) {
        validatePath(path);

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    "O caminho não corresponde a um diretório: " + path
            );
        }
    }
}