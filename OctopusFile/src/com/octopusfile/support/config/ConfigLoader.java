package com.octopusfile.support.config;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Carrega configuração de OctopusFile a partir de arquivos .properties,
 * classpath resources, ou de um Properties já em memória. Não interpreta
 * os valores (isso é responsabilidade de ConfigManager) — apenas resolve
 * a origem dos dados brutos.
 */
public class ConfigLoader {

    public Properties fromFile(Path path) {
        if (!Files.exists(path)) {
            throw new OctopusFileException(ErrorCodes.CONFIG_LOAD_ERROR, "Arquivo de configuração não encontrado", path);
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.CONFIG_LOAD_ERROR, "Falha ao ler arquivo de configuração", path, e);
        }
        return props;
    }

    public Properties fromClasspath(String resourceName) {
        Properties props = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new OctopusFileException(ErrorCodes.CONFIG_LOAD_ERROR, "Recurso de classpath não encontrado: " + resourceName);
            }
            props.load(in);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.CONFIG_LOAD_ERROR, "Falha ao ler recurso de classpath: " + resourceName, e);
        }
        return props;
    }

    public Properties fromProfile(Profiles profile) {
        Properties props = new Properties();
        props.setProperty("cache.entries", String.valueOf(profile.cacheEntries()));
        props.setProperty("cache.ttlMillis", String.valueOf(profile.cacheTtlMillis()));
        props.setProperty("validation.strict", String.valueOf(profile.strictValidation()));
        props.setProperty("io.threads", String.valueOf(profile.ioThreads()));
        return props;
    }

    public Properties merge(Properties base, Properties overrides) {
        Properties merged = new Properties();
        merged.putAll(base);
        merged.putAll(overrides);
        return merged;
    }
}
