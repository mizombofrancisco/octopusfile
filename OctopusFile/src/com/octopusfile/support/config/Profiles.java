package com.octopusfile.support.config;

/**
 * Perfis de configuração pré-definidos para cenários comuns de uso da
 * biblioteca. Cada perfil ajusta o trade-off entre desempenho, segurança
 * e uso de memória; veja ConfigManager para aplicar um perfil.
 */
public enum Profiles {

    /** Máxima performance: caches maiores, TTLs longos, menos validação de segurança. Uso: batch interno confiável. */
    PERFORMANCE(64, 30_000, false, 8),

    /** Equilíbrio entre performance e segurança — perfil padrão para a maioria das aplicações. */
    BALANCED(16, 5_000, true, 4),

    /** Máxima segurança: sandbox obrigatório, sem cache de conteúdo, validação estrita. Uso: upload de terceiros. */
    STRICT(0, 0, true, 2),

    /** Voltado a testes: caches desabilitados para resultados determinísticos, pool de threads mínimo. */
    TESTING(0, 0, true, 1);

    private final int cacheEntries;
    private final long cacheTtlMillis;
    private final boolean strictValidation;
    private final int ioThreads;

    Profiles(int cacheEntries, long cacheTtlMillis, boolean strictValidation, int ioThreads) {
        this.cacheEntries = cacheEntries;
        this.cacheTtlMillis = cacheTtlMillis;
        this.strictValidation = strictValidation;
        this.ioThreads = ioThreads;
    }

    public int cacheEntries() {
        return cacheEntries;
    }

    public long cacheTtlMillis() {
        return cacheTtlMillis;
    }

    public boolean strictValidation() {
        return strictValidation;
    }

    public int ioThreads() {
        return ioThreads;
    }
}
