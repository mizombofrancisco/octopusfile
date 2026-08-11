package com.octopusfile.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Provedor de armazenamento para dispositivos externos (ex.: pen drives,
 * HDs/SSDs externos) montados no sistema operacional.
 */
public class DispositivosExternos implements StorageProvider {

    private final Path pontoDeMontagem;

    public DispositivosExternos(Path pontoDeMontagem) {
        this.pontoDeMontagem = pontoDeMontagem;
    }

    /** Lista os pontos de montagem (raízes) atualmente reconhecidos pelo sistema. */
    public static List<Path> listarDispositivos() {
        List<Path> dispositivos = new ArrayList<>();
        for (Path root : java.nio.file.FileSystems.getDefault().getRootDirectories()) {
            dispositivos.add(root);
        }
        return dispositivos;
    }

    public FileStore getFileStore() throws IOException {
        return Files.getFileStore(pontoDeMontagem);
    }

    private Path resolver(String caminhoRelativo) {
        return pontoDeMontagem.resolve(caminhoRelativo);
    }

    @Override
    public String getNome() {
        return "Dispositivo Externo - " + pontoDeMontagem;
    }

    @Override
    public boolean isDisponivel() {
        return Files.exists(pontoDeMontagem);
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
