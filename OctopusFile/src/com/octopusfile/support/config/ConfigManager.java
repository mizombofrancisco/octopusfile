package com.octopusfile.support.config;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Detém a configuração ativa da instância OctopusFile (interpretada a
 * partir de um Properties carregado por ConfigLoader ou de um Profiles).
 * Thread-safe e "hot-swappable": a configuração pode ser recarregada em
 * tempo de execução via {@link #reload(Properties)} sem reiniciar a lib —
 * componentes que leem via {@link #current()} sempre veem o valor mais
 * recente.
 */
public class ConfigManager {

    public record Settings(
            int cacheEntries,
            long cacheTtlMillis,
            boolean strictValidation,
            int ioThreads
    ) {
        static Settings defaults() {
            return fromProfile(Profiles.BALANCED);
        }

        static Settings fromProfile(Profiles profile) {
            return new Settings(profile.cacheEntries(), profile.cacheTtlMillis(), profile.strictValidation(), profile.ioThreads());
        }
    }

    private final AtomicReference<Settings> settings;

    public ConfigManager() {
        this.settings = new AtomicReference<>(Settings.defaults());
    }

    public ConfigManager(Profiles profile) {
        this.settings = new AtomicReference<>(Settings.fromProfile(profile));
    }

    public ConfigManager(Properties properties) {
        this.settings = new AtomicReference<>(parse(properties));
    }

    public Settings current() {
        return settings.get();
    }

    public void reload(Properties properties) {
        settings.set(parse(properties));
    }

    public void applyProfile(Profiles profile) {
        settings.set(Settings.fromProfile(profile));
    }

    private Settings parse(Properties properties) {
        try {
            int cacheEntries = Integer.parseInt(properties.getProperty("cache.entries", "16"));
            long cacheTtlMillis = Long.parseLong(properties.getProperty("cache.ttlMillis", "5000"));
            boolean strictValidation = Boolean.parseBoolean(properties.getProperty("validation.strict", "true"));
            int ioThreads = Integer.parseInt(properties.getProperty("io.threads", "4"));

            if (cacheEntries < 0 || cacheTtlMillis < 0 || ioThreads < 1) {
                throw new OctopusFileException(ErrorCodes.CONFIG_INVALID, "Valores de configuração fora do intervalo permitido");
            }
            return new Settings(cacheEntries, cacheTtlMillis, strictValidation, ioThreads);
        } catch (NumberFormatException e) {
            throw new OctopusFileException(ErrorCodes.CONFIG_INVALID, "Valor numérico inválido na configuração", e);
        }
    }
}
