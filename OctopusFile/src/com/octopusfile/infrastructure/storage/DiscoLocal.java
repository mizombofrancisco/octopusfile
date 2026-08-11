package com.octopusfile.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provedor de armazenamento para o disco local (sistema de arquivos nativo),
 * baseado em {@code java.nio.file}.
 */
public class DiscoLocal implements StorageProvider {

    @Override
    public String getNome() {
        return "Disco Local";
    }

    @Override
    public boolean isDisponivel() {
        return true;
    }

    @Override
    public InputStream abrirLeitura(String caminho) throws IOException {
        return Files.newInputStream(Paths.get(caminho));
    }

    @Override
    public OutputStream abrirEscrita(String caminho) throws IOException {
        Path path = Paths.get(caminho);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        return Files.newOutputStream(path);
    }

    @Override
    public boolean remover(String caminho) throws IOException {
        return Files.deleteIfExists(Paths.get(caminho));
    }

    @Override
    public boolean existe(String caminho) {
        return Files.exists(Paths.get(caminho));
    }
}
