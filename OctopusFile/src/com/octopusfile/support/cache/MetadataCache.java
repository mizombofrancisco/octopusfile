package com.octopusfile.support.cache;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Cache em memória para metadados de arquivo (tamanho, timestamps, tipo)
 * que evita chamadas repetidas de stat() em operações que consultam o
 * mesmo arquivo várias vezes em curto intervalo (ex.: filtros + organizador
 * rodando em sequência sobre o mesmo diretório).
 *
 * Estratégia: TTL simples por entrada, sem LRU — adequado para diretórios
 * de tamanho moderado. Para árvores muito grandes, prefira desabilitar o
 * cache (ttlMillis = 0) e ler direto do disco.
 */
public class MetadataCache {

    private record Entry(BasicFileAttributes attributes, long expiresAtNanos) {
        boolean isExpired(long nowNanos) {
            return nowNanos >= expiresAtNanos;
        }
    }

    private final ConcurrentHashMap<Path, Entry> cache = new ConcurrentHashMap<>();
    private final long ttlNanos;

    public MetadataCache() {
        this(2, TimeUnit.SECONDS);
    }

    public MetadataCache(long ttl, TimeUnit unit) {
        this.ttlNanos = unit.toNanos(ttl);
    }

    /**
     * Retorna os atributos em cache, ou os calcula via {@code loader} e
     * armazena caso ausentes/expirados.
     */
    public BasicFileAttributes getOrLoad(Path path, Function<Path, BasicFileAttributes> loader) {
        long now = System.nanoTime();
        Entry entry = cache.get(path);
        if (entry != null && !entry.isExpired(now)) {
            return entry.attributes();
        }
        BasicFileAttributes attrs = loader.apply(path);
        cache.put(path, new Entry(attrs, now + ttlNanos));
        return attrs;
    }

    public void invalidate(Path path) {
        cache.remove(path);
    }

    public void invalidateAll() {
        cache.clear();
    }

    /** Remove proativamente entradas expiradas (chamado periodicamente pelo scheduler). */
    public void evictExpired() {
        long now = System.nanoTime();
        cache.entrySet().removeIf(e -> e.getValue().isExpired(now));
    }

    public int size() {
        return cache.size();
    }
}
