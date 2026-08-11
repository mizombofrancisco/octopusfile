package com.octopusfile.infrastructure.compression;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contrato para motores de compressão/descompressão de arquivos e diretórios.
 * <p>
 * Implementações devem trabalhar em modo streaming (sem carregar o conteúdo
 * inteiro em memória), para suportar arquivos grandes sem sobrecarregar o heap.
 */
public interface CompressionEngine {

    /**
     * Comprime um arquivo ou diretório (recursivamente) em um único arquivo
     * de destino.
     *
     * @param source             arquivo ou diretório de origem
     * @param destinationArchive caminho do arquivo comprimido resultante
     * @throws IOException caso ocorra falha de leitura/escrita
     */
    void compress(Path source, Path destinationArchive) throws IOException;

    /**
     * Descomprime um arquivo gerado por {@link #compress(Path, Path)},
     * restaurando o(s) arquivo(s) original(is) dentro do diretório de destino.
     *
     * @param sourceArchive         arquivo comprimido de origem
     * @param destinationDirectory  diretório onde o conteúdo será extraído
     * @throws IOException caso ocorra falha de leitura/escrita
     */
    void decompress(Path sourceArchive, Path destinationDirectory) throws IOException;
}
