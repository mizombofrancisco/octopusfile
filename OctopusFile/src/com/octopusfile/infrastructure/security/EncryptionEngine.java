package com.octopusfile.infrastructure.security;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Cifra/decifra arquivos com AES-256-GCM, derivando a chave de uma senha via
 * PBKDF2 (com salt aleatório e muitas iterações). Útil para proteger backups
 * ou itens da lixeira antes de enviá-los a um armazenamento externo.
 * <p>
 * Formato do arquivo cifrado: {@code [salt 16 bytes][iv 12 bytes][dados cifrados + tag GCM]}.
 * Todo o processo é feito em streaming, em blocos de {@value #BUFFER_SIZE} bytes.
 */
public class EncryptionEngine {

    private static final int BUFFER_SIZE = 8192;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;

    /** Cifra {@code source} para {@code destination} usando a senha informada. */
    public void encrypt(Path source, Path destination, char[] password) throws IOException {
        byte[] salt = randomBytes(SALT_LENGTH);
        byte[] iv = randomBytes(IV_LENGTH);

        try {
            SecretKey key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }

            try (InputStream in = new BufferedInputStream(Files.newInputStream(source), BUFFER_SIZE);
                 OutputStream fileOut = Files.newOutputStream(destination)) {

                fileOut.write(salt);
                fileOut.write(iv);

                try (CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int lidos;
                    while ((lidos = in.read(buffer)) != -1) {
                        cipherOut.write(buffer, 0, lidos);
                    }
                }
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Falha ao cifrar " + source, e);
        }
    }

    /** Decifra {@code source} (gerado por {@link #encrypt}) para {@code destination} usando a senha informada. */
    public void decrypt(Path source, Path destination, char[] password) throws IOException {
        try (InputStream fileIn = new BufferedInputStream(Files.newInputStream(source), BUFFER_SIZE)) {
            byte[] salt = fileIn.readNBytes(SALT_LENGTH);
            byte[] iv = fileIn.readNBytes(IV_LENGTH);

            if (salt.length != SALT_LENGTH || iv.length != IV_LENGTH) {
                throw new IOException("Arquivo cifrado inválido ou corrompido: " + source);
            }

            SecretKey key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }

            try (CipherInputStream cipherIn = new CipherInputStream(fileIn, cipher);
                 OutputStream out = new BufferedOutputStream(Files.newOutputStream(destination), BUFFER_SIZE)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int lidos;
                while ((lidos = cipherIn.read(buffer)) != -1) {
                    out.write(buffer, 0, lidos);
                }
            }
        } catch (GeneralSecurityException e) {
            throw new IOException(
                    "Falha ao decifrar " + source + " (senha incorreta ou arquivo corrompido)", e);
        }
    }

    private SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
