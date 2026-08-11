package com.octopusfile.support.cache;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Cache de listagens de diretório (resultado de Files.newDirectoryStream).
 * Diferente do MetadataCache, aqui o custo de "miss" costuma ser bem maior
 * (uma listagem completa vs. um único stat), então o TTL padrão é maior.
 *
 * Invalidação: deve ser chamada explicitamente por qualquer operação que
 * crie/mova/exclua entradas dentro do diretório (FileOrganizer, FileMover,
 * etc.), já que não há inotify/FSEvents acoplado a este cache — para
 * invalidação automática baseada em eventos reais do SO, veja FileWatcher.
 */
public class DirectoryCache {

    private record Entry(List<Path> entries, long expiresAtNanos) {
        boolean isExpired(long nowNanos) {
            return nowNanos >= expiresAtNanos;
        }
    }

    private final ConcurrentHashMap<Path, Entry> cache = new ConcurrentHashMap<>();
    private final long ttlNanos;

    public DirectoryCache() {
        this(10, TimeUnit.SECONDS);
    }

    public DirectoryCache(long ttl, TimeUnit unit) {
        this.ttlNanos = unit.toNanos(ttl);
    }

    public List<Path> getOrLoad(Path dir, Function<Path, List<Path>> loader) {
        long now = System.nanoTime();
        Entry entry = cache.get(dir);
        if (entry != null && !entry.isExpired(now)) {
            return entry.entries();
        }
        List<Path> listing = loader.apply(dir);
        cache.put(dir, new Entry(listing, now + ttlNanos));
        return listing;
    }

    public void invalidate(Path dir) {
        cache.remove(dir);
    }

    /** Invalida o diretório pai de um caminho — útil ao inserir/remover uma entrada específica. */
    public void invalidateParentOf(Path path) {
        Path parent = path.getParent();
        if (parent != null) {
            cache.remove(parent);
        }
    }

    public void invalidateAll() {
        cache.clear();
    }

    public void evictExpired() {
        long now = System.nanoTime();
        cache.entrySet().removeIf(e -> e.getValue().isExpired(now));
    }

    public int size() {
        return cache.size();
    }
}
