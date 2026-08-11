package com.octopusfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

/**
 * Ponto de acesso simplificado para operações comuns sobre arquivos.
 *
 * <p>Esta classe fornece uma API pública conveniente, enquanto
 * delega operações especializadas aos módulos internos do OctopusFile.</p>
 */
public class FileManager {

    /**
     * Cria um novo arquivo caso ele ainda não exista.
     *
     * @param path caminho do arquivo a ser criado
     * @throws IOException caso o arquivo não possa ser criado
     */
    public void create(Path path) throws IOException {
        validatePath(path);

        if (!Files.exists(path)) {
            createParentDirectories(path);
            Files.createFile(path);
        }
    }

    /**
     * Exclui um arquivo caso ele exista.
     *
     * @param path caminho do arquivo a ser excluído
     * @throws IOException caso o arquivo não possa ser excluído
     */
    public void delete(Path path) throws IOException {
        validatePath(path);
        Files.deleteIfExists(path);
    }

    /**
     * Verifica se um caminho existe.
     *
     * @param path caminho a verificar
     * @return true se o caminho existir
     */
    public boolean exists(Path path) {
        return path != null && Files.exists(path);
    }

    /**
     * Verifica se o caminho representa um arquivo regular.
     *
     * @param path caminho a verificar
     * @return true se o caminho for um arquivo
     */
    public boolean isFile(Path path) {
        return path != null && Files.isRegularFile(path);
    }

    /**
     * Verifica se o caminho representa um diretório.
     *
     * @param path caminho a verificar
     * @return true se o caminho for um diretório
     */
    public boolean isDirectory(Path path) {
        return path != null && Files.isDirectory(path);
    }

    /**
     * Cria os diretórios necessários para o arquivo.
     *
     * @param path caminho do arquivo
     * @throws IOException caso os diretórios não possam ser criados
     */
    public void createParentDirectories(Path path) throws IOException {
        validatePath(path);

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Cria um arquivo juntamente com os diretórios necessários.
     *
     * @param path caminho do arquivo
     * @throws IOException caso o arquivo ou diretórios não possam ser criados
     */
    public void createWithDirectories(Path path) throws IOException {
        createParentDirectories(path);
        create(path);
    }

    /**
     * Copia um arquivo para outro caminho.
     *
     * @param source arquivo de origem
     * @param target caminho de destino
     * @throws IOException caso o arquivo não possa ser copiado
     */
    public void copy(Path source, Path target) throws IOException {
        validatePath(source);
        validatePath(target);

        createParentDirectories(target);

        Files.copy(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    /**
     * Move um arquivo para outro caminho.
     *
     * @param source arquivo de origem
     * @param target caminho de destino
     * @throws IOException caso o arquivo não possa ser movido
     */
    public void move(Path source, Path target) throws IOException {
        validatePath(source);
        validatePath(target);

        createParentDirectories(target);

        Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    /**
     * Renomeia um arquivo.
     *
     * @param source caminho atual do arquivo
     * @param target novo caminho do arquivo
     * @throws IOException caso o arquivo não possa ser renomeado
     */
    public void rename(Path source, Path target) throws IOException {
        move(source, target);
    }

    /**
     * Lê todo o conteúdo do arquivo como texto UTF-8.
     *
     * @param path caminho do arquivo
     * @return conteúdo do arquivo
     * @throws IOException caso o arquivo não possa ser lido
     */
    public String read(Path path) throws IOException {
        validatePath(path);

        return Files.readString(
                path,
                StandardCharsets.UTF_8
        );
    }

    /**
     * Lê todas as linhas de um arquivo de texto UTF-8.
     *
     * @param path caminho do arquivo
     * @return lista contendo as linhas do arquivo
     * @throws IOException caso o arquivo não possa ser lido
     */
    public List<String> readLines(Path path) throws IOException {
        validatePath(path);

        return Files.readAllLines(
                path,
                StandardCharsets.UTF_8
        );
    }

    /**
     * Escreve texto UTF-8 em um arquivo.
     *
     * O conteúdo existente será substituído.
     *
     * @param path caminho do arquivo
     * @param content conteúdo a ser escrito
     * @throws IOException caso o arquivo não possa ser escrito
     */
    public void write(Path path, String content) throws IOException {
        validatePath(path);

        createParentDirectories(path);

        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Adiciona texto UTF-8 ao final de um arquivo.
     *
     * @param path caminho do arquivo
     * @param content conteúdo a adicionar
     * @throws IOException caso o arquivo não possa ser escrito
     */
    public void append(Path path, String content) throws IOException {
        validatePath(path);

        createParentDirectories(path);

        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    /**
     * Lê os bytes brutos de um arquivo.
     *
     * @param path caminho do arquivo
     * @return bytes do arquivo
     * @throws IOException caso o arquivo não possa ser lido
     */
    public byte[] readBytes(Path path) throws IOException {
        validatePath(path);

        return Files.readAllBytes(path);
    }

    /**
     * Escreve bytes brutos em um arquivo.
     *
     * @param path caminho do arquivo
     * @param data dados a serem escritos
     * @throws IOException caso o arquivo não possa ser escrito
     */
    public void writeBytes(Path path, byte[] data) throws IOException {
        validatePath(path);

        createParentDirectories(path);

        Files.write(
                path,
                data,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Retorna o tamanho do arquivo em bytes.
     *
     * @param path caminho do arquivo
     * @return tamanho do arquivo
     * @throws IOException caso o arquivo não possa ser acessado
     */
    public long size(Path path) throws IOException {
        validatePath(path);

        return Files.size(path);
    }

    /**
     * Retorna a data e hora da última modificação do arquivo.
     *
     * @param path caminho do arquivo
     * @return instante da última modificação
     * @throws IOException caso o arquivo não possa ser acessado
     */
    public Instant lastModified(Path path) throws IOException {
        validatePath(path);

        return Files.getLastModifiedTime(path).toInstant();
    }

    /**
     * Verifica se o arquivo pode ser lido.
     *
     * @param path caminho do arquivo
     * @return true se o arquivo puder ser lido
     */
    public boolean isReadable(Path path) {
        return path != null && Files.isReadable(path);
    }

    /**
     * Verifica se o arquivo pode ser escrito.
     *
     * @param path caminho do arquivo
     * @return true se o arquivo puder ser escrito
     */
    public boolean isWritable(Path path) {
        return path != null && Files.isWritable(path);
    }

    /**
     * Verifica se o arquivo pode ser executado.
     *
     * @param path caminho do arquivo
     * @return true se o arquivo puder ser executado
     */
    public boolean isExecutable(Path path) {
        return path != null && Files.isExecutable(path);
    }

    /**
     * Retorna o nome do arquivo.
     *
     * @param path caminho do arquivo
     * @return nome do arquivo
     */
    public String getFileName(Path path) {
        validatePath(path);

        Path fileName = path.getFileName();

        return fileName != null
                ? fileName.toString()
                : "";
    }

    /**
     * Retorna a extensão do arquivo sem o ponto.
     *
     * @param path caminho do arquivo
     * @return extensão do arquivo ou uma string vazia
     */
    public String getExtension(Path path) {
        validatePath(path);

        String fileName = getFileName(path);
        int index = fileName.lastIndexOf('.');

        if (index <= 0 || index == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(index + 1);
    }

    /**
     * Valida o caminho antes de executar uma operação.
     */
    private void validatePath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException(
                    "O caminho não pode ser nulo"
            );
        }
    }
}
