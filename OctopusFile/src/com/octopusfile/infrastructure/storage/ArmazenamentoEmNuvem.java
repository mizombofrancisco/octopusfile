package com.octopusfile.infrastructure.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provedor de armazenamento em nuvem (ex.: S3, Azure Blob, GCS).
 * <p>
 * Esta implementação define os pontos de extensão necessários para integrar
 * um SDK real de nuvem (ex.: AWS SDK) através do método {@link #getBucket()}
 * e mantém, por padrão, um backend em memória para uso e testes locais.
 */
public class ArmazenamentoEmNuvem implements StorageProvider {

    private final String bucket;
    private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();

    public ArmazenamentoEmNuvem(String bucket) {
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }

    @Override
    public String getNome() {
        return "Armazenamento em Nuvem - bucket: " + bucket;
    }

    @Override
    public boolean isDisponivel() {
        // Ponto de extensão: substituir por verificação real de credenciais/conectividade.
        return true;
    }

    @Override
    public InputStream abrirLeitura(String caminho) throws IOException {
        byte[] dados = objetos.get(caminho);
        if (dados == null) {
            throw new IOException("Objeto não encontrado no bucket '" + bucket + "': " + caminho);
        }
        return new ByteArrayInputStream(dados);
    }

    @Override
    public OutputStream abrirEscrita(String caminho) {
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                objetos.put(caminho, this.toByteArray());
            }
        };
    }

    @Override
    public boolean remover(String caminho) {
        return objetos.remove(caminho) != null;
    }

    @Override
    public boolean existe(String caminho) {
        return objetos.containsKey(caminho);
    }
}
