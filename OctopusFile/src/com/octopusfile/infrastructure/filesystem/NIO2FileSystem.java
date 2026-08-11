package com.octopusfile.infrastructure.filesystem;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.ExceptionHandler;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Camada única de acesso a java.nio.file usada por toda a biblioteca.
 * Nenhum outro módulo deve chamar {@code java.nio.file.Files} diretamente —
 * tudo passa por aqui. Isso garante que:
 *  - erros são sempre traduzidos para OctopusFileException;
 *  - existe um ponto único para trocar a implementação de storage
 *    (disco local, SMB/NFS, nuvem) sem tocar nos módulos de negócio.
 */
public class NIO2FileSystem {

    /** Lê o conteúdo inteiro de um arquivo texto usando o charset padrão UTF-8. */
    public String readString(Path path) {
        return ExceptionHandler.run(path, () -> Files.readString(path));
    }

    public byte[] readAllBytes(Path path) {
        return ExceptionHandler.run(path, () -> Files.readAllBytes(path));
    }

    public List<String> readAllLines(Path path) {
        return ExceptionHandler.run(path, () -> Files.readAllLines(path));
    }

    public InputStream newInputStream(Path path, OpenOption... options) {
        return ExceptionHandler.run(path, () -> Files.newInputStream(path, options));
    }

    public OutputStream newOutputStream(Path path, OpenOption... options) {
        return ExceptionHandler.run(path, () -> {
            ensureParentDirectoryExists(path);
            return Files.newOutputStream(path, options);
        });
    }

    public Path writeString(Path path, String content, OpenOption... options) {
        return ExceptionHandler.run(path, () -> {
            ensureParentDirectoryExists(path);
            return Files.writeString(path, content, options.length == 0
                    ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
                    : options);
        });
    }

    public Path writeBytes(Path path, byte[] content, OpenOption... options) {
        return ExceptionHandler.run(path, () -> {
            ensureParentDirectoryExists(path);
            return Files.write(path, content, options.length == 0
                    ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
                    : options);
        });
    }

    public void ensureParentDirectoryExists(Path path) {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            ExceptionHandler.run(path, () -> Files.createDirectories(parent));
        }
    }

    public Path createDirectories(Path dir) {
        return ExceptionHandler.run(dir, () -> Files.createDirectories(dir));
    }

    public Path copy(Path source, Path target, CopyOption... options) {
        return ExceptionHandler.run(source, () -> {
            ensureParentDirectoryExists(target);
            return Files.copy(source, target, options);
        });
    }

    public Path move(Path source, Path target, CopyOption... options) {
        return ExceptionHandler.run(source, () -> {
            ensureParentDirectoryExists(target);
            return Files.move(source, target, options);
        });
    }

    public boolean deleteIfExists(Path path) {
        return ExceptionHandler.run(path, () -> Files.deleteIfExists(path));
    }

    /** Exclui recursivamente um diretório e todo o seu conteúdo. */
    public void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        ExceptionHandler.run(root, () -> {
            try (Stream<Path> walk = Files.walk(root)) {
                List<Path> ordered = walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
                for (Path p : ordered) {
                    Files.deleteIfExists(p);
                }
            }
        });
    }

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    public boolean isRegularFile(Path path) {
        return Files.isRegularFile(path);
    }

    public long size(Path path) {
        return ExceptionHandler.run(path, () -> Files.size(path));
    }

    public BasicFileAttributes readAttributes(Path path) {
        return ExceptionHandler.run(path, () -> Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
    }

    public FileTime lastModifiedTime(Path path) {
        return ExceptionHandler.run(path, () -> Files.getLastModifiedTime(path));
    }

    /** Lista as entradas diretas (não recursivo) de um diretório. */
    public List<Path> listDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new OctopusFileException(ErrorCodes.NOT_A_DIRECTORY, null, dir);
        }
        return ExceptionHandler.run(dir, () -> {
            List<Path> entries = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) {
                    entries.add(p);
                }
            }
            return entries;
        });
    }

    /** Percorre recursivamente um diretório retornando todos os arquivos e subdiretórios. */
    public List<Path> walk(Path root) {
        return ExceptionHandler.run(root, () -> {
            try (Stream<Path> stream = Files.walk(root)) {
                return stream.toList();
            }
        });
    }

    public WatchService newWatchService(Path anyPath) {
        return ExceptionHandler.run(anyPath, () -> anyPath.getFileSystem().newWatchService());
    }

    /** Atalho para operações de move usadas como "rename" (mesmo diretório pai). */
    public Path rename(Path source, String newName) {
        Path target = source.resolveSibling(newName);
        return move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
