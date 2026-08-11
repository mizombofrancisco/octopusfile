package com.octopusfile.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Registro extensível para provedores de armazenamento customizados,
 * permitindo plugar novas implementações de {@link StorageProvider}
 * (ex.: FTP, WebDAV, provedores proprietários) sem alterar o núcleo
 * da biblioteca.
 */
public class OutrosProvedores {

    private final Map<String, StorageProvider> provedoresCustomizados = new HashMap<>();

    public void registrar(String chave, StorageProvider provider) {
        provedoresCustomizados.put(chave, provider);
    }

    public StorageProvider obter(String chave) {
        StorageProvider provider = provedoresCustomizados.get(chave);
        if (provider == null) {
            throw new IllegalArgumentException("Provedor não registrado: " + chave);
        }
        return provider;
    }

    public boolean isRegistrado(String chave) {
        return provedoresCustomizados.containsKey(chave);
    }

    public InputStream abrirLeitura(String chave, String caminho) throws IOException {
        return obter(chave).abrirLeitura(caminho);
    }

    public OutputStream abrirEscrita(String chave, String caminho) throws IOException {
        return obter(chave).abrirEscrita(caminho);
    }
}
