package com.octopusfile.infrastructure.compression;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Implementação de {@link CompressionEngine} baseada em ZIP (java.util.zip),
 * sem qualquer dependência externa.
 * <p>
 * Todo o processo é feito em modo streaming, lendo e escrevendo em blocos de
 * {@value #BUFFER_SIZE} bytes: o arquivo original nunca é carregado por
 * completo em memória, mesmo quando muito grande — apenas o bloco atual
 * transita pelo heap.
 */
public class ZipCompressionEngine implements CompressionEngine {

    private static final int BUFFER_SIZE = 8192;

    private final int compressionLevel;

    /** Cria o motor usando o nível de compressão máximo (menor tamanho, mais CPU). */
    public ZipCompressionEngine() {
        this(Deflater.BEST_COMPRESSION);
    }

    /**
     * Cria o motor com um nível de compressão customizado.
     *
     * @param compressionLevel valor entre {@link Deflater#NO_COMPRESSION} (0)
     *                         e {@link Deflater#BEST_COMPRESSION} (9)
     */
    public ZipCompressionEngine(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    @Override
    public void compress(Path source, Path destinationArchive) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Origem não encontrada: " + source);
        }

        if (destinationArchive.getParent() != null) {
            Files.createDirectories(destinationArchive.getParent());
        }

        try (OutputStream fileOut = Files.newOutputStream(destinationArchive);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut, BUFFER_SIZE);
             ZipOutputStream zipOut = new ZipOutputStream(bufferedOut)) {

            zipOut.setLevel(compressionLevel);

            if (Files.isDirectory(source)) {
                try (Stream<Path> stream = Files.walk(source)) {
                    for (Path path : (Iterable<Path>) stream::iterator) {
                        String entryName = source.relativize(path).toString().replace('\\', '/');

                        if (entryName.isEmpty()) {
                            continue; // é o próprio diretório-raiz
                        }

                        if (Files.isDirectory(path)) {
                            zipOut.putNextEntry(new ZipEntry(entryName + "/"));
                            zipOut.closeEntry();
                        } else {
                            addFileToZip(path, entryName, zipOut);
                        }
                    }
                }
            } else {
                addFileToZip(source, source.getFileName().toString(), zipOut);
            }
        }
    }

    private void addFileToZip(Path file, String entryName, ZipOutputStream zipOut) throws IOException {
        zipOut.putNextEntry(new ZipEntry(entryName));

        try (InputStream fileIn = Files.newInputStream(file);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn, BUFFER_SIZE)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int lidos;
            while ((lidos = bufferedIn.read(buffer)) != -1) {
                zipOut.write(buffer, 0, lidos);
            }
        }

        zipOut.closeEntry();
    }

    @Override
    public void decompress(Path sourceArchive, Path destinationDirectory) throws IOException {
        Files.createDirectories(destinationDirectory);
        Path destinoNormalizado = destinationDirectory.normalize();

        try (InputStream fileIn = Files.newInputStream(sourceArchive);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn, BUFFER_SIZE);
             ZipInputStream zipIn = new ZipInputStream(bufferedIn)) {

            ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];

            while ((entry = zipIn.getNextEntry()) != null) {
                Path target = resolveSafely(destinoNormalizado, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }

                    try (OutputStream fileOut = Files.newOutputStream(target);
                         BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut, BUFFER_SIZE)) {

                        int lidos;
                        while ((lidos = zipIn.read(buffer)) != -1) {
                            bufferedOut.write(buffer, 0, lidos);
                        }
                    }
                }

                zipIn.closeEntry();
            }
        }
    }

    /**
     * Resolve o caminho de destino de uma entrada do ZIP, prevenindo ataques
     * de "Zip Slip" (entradas com "../" que tentariam escrever fora do
     * diretório de destino).
     */
    private Path resolveSafely(Path destinationDirectory, String entryName) throws IOException {
        Path target = destinationDirectory.resolve(entryName).normalize();

        if (!target.startsWith(destinationDirectory)) {
            throw new IOException("Entrada de arquivo suspeita (zip slip): " + entryName);
        }

        return target;
    }
}
