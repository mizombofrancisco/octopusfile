package com.octopusfile.modules.backup;

import com.octopusfile.infrastructure.compression.CompressionEngine;
import com.octopusfile.infrastructure.compression.ZipCompressionEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Serviço responsável por criar e restaurar backups comprimidos de arquivos
 * e diretórios.
 * <p>
 * O conteúdo é sempre processado em modo streaming pelo {@link CompressionEngine}
 * configurado, evitando carregar arquivos grandes inteiramente em memória —
 * mesmo diretórios com muitos gigabytes são copiados em blocos pequenos.
 */
public class BackupService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");

    private final CompressionEngine compressionEngine;

    public BackupService() {
        this(new ZipCompressionEngine());
    }

    public BackupService(CompressionEngine compressionEngine) {
        this.compressionEngine = compressionEngine;
    }

    /**
     * Cria um backup comprimido de {@code source} dentro de {@code backupDirectory},
     * nomeado com o nome original mais um timestamp (para nunca sobrescrever
     * um backup anterior por engano).
     *
     * @param source          arquivo ou diretório a ser copiado com segurança
     * @param backupDirectory diretório onde o arquivo .zip de backup será salvo
     * @return metadados do backup gerado
     * @throws IOException caso o backup não possa ser criado
     */
    public BackupMetadata backup(Path source, Path backupDirectory) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Origem do backup não encontrada: " + source);
        }

        Files.createDirectories(backupDirectory);

        String nomeBase = source.getFileName() != null
                ? source.getFileName().toString()
                : "backup";

        String timestamp = TIMESTAMP_FORMAT.format(Instant.now().atZone(ZoneId.systemDefault()));
        Path backupPath = backupDirectory.resolve(nomeBase + "-" + timestamp + ".zip");

        compressionEngine.compress(source, backupPath);

        return new BackupMetadata(
                source,
                backupPath,
                Instant.now(),
                calculateSize(source),
                Files.size(backupPath)
        );
    }

    /**
     * Restaura um backup previamente criado, descomprimindo seu conteúdo
     * dentro do diretório informado.
     *
     * @param backupPath arquivo .zip gerado por {@link #backup(Path, Path)}
     * @param restoreTo  diretório onde o conteúdo original será restaurado
     * @throws IOException caso o backup não possa ser restaurado
     */
    public void restore(Path backupPath, Path restoreTo) throws IOException {
        if (!Files.exists(backupPath)) {
            throw new IOException("Arquivo de backup não encontrado: " + backupPath);
        }

        compressionEngine.decompress(backupPath, restoreTo);
    }

    private long calculateSize(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return Files.size(path);
        }

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
}
