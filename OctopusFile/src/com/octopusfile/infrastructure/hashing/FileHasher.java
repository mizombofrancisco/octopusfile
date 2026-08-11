package com.octopusfile.infrastructure.hashing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Calcula o hash (checksum) de um arquivo em modo streaming — nunca carrega
 * o conteúdo inteiro em memória. Útil para verificar a integridade de
 * backups/restaurações e para detectar arquivos duplicados.
 */
public class FileHasher {

    private static final int BUFFER_SIZE = 8192;

    private final String algorithm;

    public FileHasher() {
        this("SHA-256");
    }

    /** @param algorithm nome de algoritmo aceito por {@link MessageDigest}, ex.: "SHA-256", "MD5" */
    public FileHasher(String algorithm) {
        this.algorithm = algorithm;
    }

    /** Calcula o hash do arquivo, lendo em blocos pequenos. */
    public String hash(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Algoritmo de hash indisponível: " + algorithm, e);
        }

        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int lidos;
            while ((lidos = in.read(buffer)) != -1) {
                digest.update(buffer, 0, lidos);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    /** Verifica se o arquivo corresponde ao hash esperado (ex.: após restaurar um backup). */
    public boolean matches(Path file, String expectedHash) throws IOException {
        return hash(file).equalsIgnoreCase(expectedHash);
    }
}
