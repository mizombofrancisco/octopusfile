package com.octopusfile.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provedor de armazenamento para compartilhamentos de rede (SMB/CIFS ou NFS).
 * A conexão é representada por um caminho UNC (ex.: {@code \\servidor\\share})
 * ou por um ponto de montagem NFS já acessível pelo sistema operacional.
 */
public class RedeSMBNFS implements StorageProvider {

    private final String host;
    private final String compartilhamento;

    public RedeSMBNFS(String host, String compartilhamento) {
        this.host = host;
        this.compartilhamento = compartilhamento;
    }

    private Path resolver(String caminhoRelativo) {
        FileSystem fs = FileSystems.getDefault();
        String base = "//" + host + "/" + compartilhamento;
        return fs.getPath(base, caminhoRelativo);
    }

    @Override
    public String getNome() {
        return "Rede (SMB/NFS) - " + host + "/" + compartilhamento;
    }

    @Override
    public boolean isDisponivel() {
        return Files.exists(resolver(""));
    }

    @Override
    public InputStream abrirLeitura(String caminho) throws IOException {
        return Files.newInputStream(resolver(caminho));
    }

    @Override
    public OutputStream abrirEscrita(String caminho) throws IOException {
        Path path = resolver(caminho);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        return Files.newOutputStream(path);
    }

    @Override
    public boolean remover(String caminho) throws IOException {
        return Files.deleteIfExists(resolver(caminho));
    }

    @Override
    public boolean existe(String caminho) {
        return Files.exists(resolver(caminho));
    }
}
