package com.octopusfile.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Contrato comum para os provedores de armazenamento suportados pela
 * biblioteca (disco local, rede, nuvem, dispositivos externos, etc).
 */
public interface StorageProvider {

    /** Nome/identificador do provedor. */
    String getNome();

    /** Verifica se o provedor está disponível/acessível no momento. */
    boolean isDisponivel();

    /** Abre um stream de leitura para o recurso identificado por {@code caminho}. */
    InputStream abrirLeitura(String caminho) throws IOException;

    /** Abre um stream de escrita para o recurso identificado por {@code caminho}. */
    OutputStream abrirEscrita(String caminho) throws IOException;

    /** Remove o recurso identificado por {@code caminho}. */
    boolean remover(String caminho) throws IOException;

    /** Verifica se o recurso identificado por {@code caminho} existe. */
    boolean existe(String caminho);
}
